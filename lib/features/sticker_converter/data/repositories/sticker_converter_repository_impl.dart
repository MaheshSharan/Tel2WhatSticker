import 'dart:io';
import 'dart:async';
import 'package:dartz/dartz.dart';
import 'package:injectable/injectable.dart';
import 'package:archive/archive.dart';
import '../../domain/entities/sticker_pack_entity.dart';
import '../../domain/repositories/sticker_converter_repository.dart';
import '../datasources/image_processing_service.dart';
import '../datasources/telegram_api_service.dart';
import '../datasources/whatsapp_service.dart';
import '../models/sticker_pack_model.dart';
import '../../../../core/error/failures.dart';
import '../../../../core/error/exceptions.dart';
import '../../../../core/constants/app_constants.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as p;

@LazySingleton(as: StickerConverterRepository)
class StickerConverterRepositoryImpl implements StickerConverterRepository {
  final ImageProcessingService _imageProcessingService;
  final TelegramApiService _telegramApiService;
  final WhatsAppService _whatsAppService;
  
  final StreamController<ProcessingState> _progressController =
      StreamController<ProcessingState>.broadcast();
  
  ProcessingState _currentState = const ProcessingState();
  
  StickerConverterRepositoryImpl(
    this._imageProcessingService,
    this._telegramApiService,
    this._whatsAppService,
  );
  
  @override
  Future<Either<Failure, StickerPackEntity>> processImages({
    required List<File> images,
    required String packName,
    required String publisher,
  }) async {
    try {
      _updateProgress(_currentState.copyWith(
        status: ProcessingStatus.processing,
        totalFiles: images.length,
        completedFiles: 0,
        errorFiles: 0,
        fileProgress: {},
      ));
      
      // Validate files first
      final validationResult = await validateFiles(images);
      if (validationResult.isLeft()) {
        return Left(validationResult.fold((l) => l, (r) => throw Exception()));
      }
      
      // Create the final WhatsApp sticker pack directory using the identifier
      final appDir = await getApplicationDocumentsDirectory();
      final identifier = DateTime.now().millisecondsSinceEpoch.toString();
      final finalPackDir = Directory('${appDir.path}/sticker_packs/$identifier');
      if (!await finalPackDir.exists()) {
        await finalPackDir.create(recursive: true);
      }
      
      // Process images one at a time to reduce memory usage
      final processedStickers = <StickerModel>[];
      final skippedStickers = <String>[];
      bool hasAnimatedStickers = false;
      bool hasStaticStickers = false;
      
      for (int i = 0; i < images.length; i++) {
        final file = images[i];
        print('\n--- Processing image ${i + 1}/${images.length}: ${file.path} ---');
        
        try {
          // Update progress for this file
          _updateFileProgress(file.path, 0.0);
          
          // Check if this is an animated sticker
          final isAnimated = await _imageProcessingService.isAnimatedFile(file.path);
          if (isAnimated) {
            hasAnimatedStickers = true;
            print('Detected animated sticker: ${file.path}');
          } else {
            hasStaticStickers = true;
          }
          
          // Enforce that a pack cannot mix static and animated stickers
          if (hasAnimatedStickers && hasStaticStickers) {
            throw ProcessingException('A sticker pack cannot contain both static and animated stickers. Please use only static or only animated stickers in a single pack.');
          }
          
          // Determine output file extension based on input and animation
          final inputExtension = file.path.toLowerCase().split('.').last;
          String outputExtension;
          
          if (isAnimated && inputExtension == 'gif') {
            outputExtension = 'webp'; // Convert animated GIF to WebP
          } else if (isAnimated && inputExtension == 'webp') {
            outputExtension = 'webp'; // Keep animated WebP
          } else {
            outputExtension = 'jpg'; // Static formats to JPEG
          }
          
          final fileName = '${i}_sticker.$outputExtension';
          final outputPath = '${finalPackDir.path}/$fileName';
          
          _updateFileProgress(file.path, 0.25);
          
          // Process image with memory-efficient approach
          print('Processing with target format: $outputExtension');
          final processedBytes = await _imageProcessingService.processImageForWhatsApp(
            file.path,
          );
          
          _updateFileProgress(file.path, 0.75);
          
          // Save processed image
          final outputFile = File(outputPath);
          await outputFile.writeAsBytes(processedBytes);
          
          // Validate file size and requirements
          final fileSizeBytes = processedBytes.length;
          final fileSizeKB = (fileSizeBytes / 1024).round();
          
          print('Processed sticker: ${file.path} -> ${fileSizeKB}KB (${isAnimated ? 'animated' : 'static'})');
          
          // Validate individual sticker size (WhatsApp requirement: static < 100KB, animated < 500KB)
          final maxStickerSizeKB = isAnimated ? 500 : 100;
          if (fileSizeKB > maxStickerSizeKB) {
            print('ERROR: Sticker exceeds ${maxStickerSizeKB}KB limit: ${fileSizeKB}KB');
            throw ProcessingException('Sticker file size (${fileSizeKB}KB) exceeds WhatsApp limit of ${maxStickerSizeKB}KB');
          }
          
          if (fileSizeKB <= 0) {
            print('ERROR: Processed sticker has invalid size: ${fileSizeKB}KB');
            throw ProcessingException('Processed sticker file is empty or invalid');
          }
          
          print('✓ Sticker size validation passed: ${fileSizeKB}KB (limit: ${maxStickerSizeKB}KB)');
          
          // Get image dimensions from original file for metadata
          final dimensions = await _imageProcessingService.getImageDimensions(
            file.path  // Use original file path for dimension checking
          );
          
          // Create sticker model
          final sticker = StickerModel(
            imagePath: outputPath,
            emojis: ['😀'], // Default emoji, can be customized later
            fileSizeKB: fileSizeKB,
            width: dimensions['width']!,
            height: dimensions['height']!,
          );
          
          processedStickers.add(sticker);
          
          _updateFileProgress(file.path, 1.0);
          _updateProgress(_currentState.copyWith(
            completedFiles: _currentState.completedFiles + 1,
          ));
          
          // Force garbage collection after each image to free memory
          print('Image ${i + 1} processed successfully. Memory cleanup...');
          
          // Add a small delay to allow garbage collection
          await Future.delayed(const Duration(milliseconds: 100));
          
        } catch (e) {
          print('Error processing image ${file.path}: ${e.toString()}');
          skippedStickers.add(file.path);
          _updateProgress(_currentState.copyWith(
            errorFiles: _currentState.errorFiles + 1,
          ));
          // Continue processing other files even if one fails
          continue;
        }
      }
      
      if (processedStickers.isEmpty) {
        return const Left(ProcessingFailure('No stickers could be processed'));
      }
      
      if (skippedStickers.isNotEmpty) {
        print('\n=== SKIPPED STICKERS ===');
        for (final skipped in skippedStickers) {
          print('Sticker not included due to conversion error: $skipped');
        }
        print('========================\n');
      }
      
      // Log final sticker pack summary
      print('\n=== STICKER PACK PROCESSING SUMMARY ===');
      print('Total stickers processed: ${processedStickers.length}');
      print('Individual sticker sizes:');
      
      double totalSizeKB = 0;
      int stickerIndex = 1;
      
      for (final sticker in processedStickers) {
        totalSizeKB += sticker.fileSizeKB;
        print('  Sticker $stickerIndex: ${sticker.fileSizeKB}KB');
        stickerIndex++;
      }
      
      print('Total pack size: ${totalSizeKB.toStringAsFixed(1)}KB');
      print('Average sticker size: ${(totalSizeKB / processedStickers.length).toStringAsFixed(1)}KB');
      if (hasAnimatedStickers) {
        print('All stickers meet WhatsApp size requirement: ✓ (each < 500KB)');
      } else {
        print('All stickers meet WhatsApp size requirement: ✓ (each < 100KB)');
      }
      print('=====================================\n');
      
      // Create tray image directly in the final sticker pack directory
      final trayImageBytes = await File(processedStickers.first.imagePath).readAsBytes();
      final compressedTrayBytes = await _imageProcessingService.createTrayImage(trayImageBytes);
      final trayPath = p.join(finalPackDir.path, 'tray.png');
      await File(trayPath).writeAsBytes(compressedTrayBytes, flush: true);
      // Debug log to confirm tray image exists at the expected location
      final trayFile = File(trayPath);
      final trayExists = await trayFile.exists();
      final traySize = trayExists ? await trayFile.length() : -1;
      print('DEBUG: Final tray image path: $trayPath, exists: $trayExists, size: ${traySize / 1024} KB');
      // Log first 16 bytes of tray image
      if (await trayFile.exists()) {
        final trayBytes = await trayFile.readAsBytes();
        final trayHeader = trayBytes.take(16).toList();
        print('DEBUG: tray.png first 16 bytes: $trayHeader');
      }
      // Log first 16 bytes of first sticker file
      if (processedStickers.isNotEmpty) {
        final firstStickerFile = File(processedStickers.first.imagePath);
        if (await firstStickerFile.exists()) {
          final stickerBytes = await firstStickerFile.readAsBytes();
          final stickerHeader = stickerBytes.take(16).toList();
          print('DEBUG: first sticker first 16 bytes: $stickerHeader');
        }
      }
      // Create sticker pack with proper animated flag
      final pack = StickerPackModel(
        identifier: identifier,
        name: packName,
        publisher: publisher,
        trayImagePath: trayPath,
        stickers: processedStickers,
        animated: hasAnimatedStickers,
      );
      
      _updateProgress(_currentState.copyWith(
        status: ProcessingStatus.completed,
      ));
      
      return Right(pack.toEntity());
      
    } on ProcessingException catch (e) {
      _updateProgress(_currentState.copyWith(
        status: ProcessingStatus.error,
        error: e.message,
      ));
      return Left(ProcessingFailure(e.message));
    } on CacheException catch (e) {
      return Left(CacheFailure(e.message));
    } catch (e) {
      _updateProgress(_currentState.copyWith(
        status: ProcessingStatus.error,
        error: e.toString(),
      ));
      return Left(ProcessingFailure('Unexpected error: ${e.toString()}'));
    }
  }
  
  // Download Telegram stickers and return as File list for unified processing
  Future<Either<Failure, Map<String, dynamic>>> downloadTelegramStickers(String url) async {
    try {
      if (!_telegramApiService.isValidTelegramUrl(url)) {
        return const Left(ValidationFailure('Invalid Telegram sticker pack URL'));
      }
      
      final packName = _telegramApiService.extractPackNameFromUrl(url);
      if (packName == null) {
        return const Left(ValidationFailure('Could not extract pack name from URL'));
      }
      
      // Get sticker pack info from Telegram
      final stickerPackInfo = await _telegramApiService.getStickerPackInfo(packName);
      final stickers = stickerPackInfo['stickers'] as List<dynamic>?;
      
      if (stickers == null || stickers.isEmpty) {
        return const Left(TelegramFailure('No stickers found in this Telegram pack.'));
      }
      
      // Create downloads directory
      final appDir = await getApplicationDocumentsDirectory();
      final downloadDir = Directory('${appDir.path}/telegram_downloads');
      if (!await downloadDir.exists()) {
        await downloadDir.create(recursive: true);
      }
      
      // Download all stickers to files
      final downloadedFiles = <File>[];
      final downloadedStickers = <Map<String, dynamic>>[];
      
      _updateProgress(_currentState.copyWith(
        status: ProcessingStatus.processing,
        totalFiles: stickers.length,
        completedFiles: 0,
        errorFiles: 0,
      ));
      
      for (int i = 0; i < stickers.length; i++) {
        final stickerInfo = stickers[i];
        try {
          final fileId = stickerInfo['file_id'] as String;
          final emoji = stickerInfo['emoji'] as String? ?? '😀';
          final isAnimated = stickerInfo['is_animated'] == true;
          final isVideo = stickerInfo['is_video'] == true;
          
          // Determine file extension based on sticker type
          String fileName;
          if (isVideo) {
            fileName = '${i}_telegram_sticker.webm';
          } else if (isAnimated) {
            fileName = '${i}_telegram_sticker.webp'; // Animated WebP
          } else {
            fileName = '${i}_telegram_sticker.webp'; // Static WebP
          }
          
          final outputPath = '${downloadDir.path}/$fileName';
          
          // Download sticker file
          final stickerFile = await _telegramApiService.downloadSticker(
            fileId: fileId,
            fileName: fileName,
          );
          
          // Move to our expected location if different
          if (stickerFile.path != outputPath) {
            await stickerFile.copy(outputPath);
            await stickerFile.delete();
          }
          
          final downloadedFile = File(outputPath);
          if (await downloadedFile.exists()) {
            downloadedFiles.add(downloadedFile);
            downloadedStickers.add({
              'file_id': fileId,
              'emoji': emoji,
              'is_animated': isAnimated,
              'is_video': isVideo,
              'local_path': outputPath,
            });
          }
          
          _updateProgress(_currentState.copyWith(
            completedFiles: _currentState.completedFiles + 1,
          ));
          
        } catch (e) {
          print('Error downloading sticker $i: ${e.toString()}');
          _updateProgress(_currentState.copyWith(
            errorFiles: _currentState.errorFiles + 1,
          ));
          continue;
        }
      }
      
      if (downloadedFiles.isEmpty) {
        return const Left(TelegramFailure('No stickers could be downloaded.'));
      }
      
      _updateProgress(_currentState.copyWith(
        status: ProcessingStatus.completed,
      ));
      
      return Right({
        'files': downloadedFiles,
        'pack_name': stickerPackInfo['name'] ?? packName,
        'title': stickerPackInfo['title'] ?? packName,
        'stickers': downloadedStickers,
      });
      
    } on TelegramException catch (e) {
      _updateProgress(_currentState.copyWith(
        status: ProcessingStatus.error,
        error: e.message,
      ));
      return Left(TelegramFailure(e.message));
    } on NetworkException catch (e) {
      return Left(NetworkFailure(e.message));
    } catch (e) {
      _updateProgress(_currentState.copyWith(
        status: ProcessingStatus.error,
        error: e.toString(),
      ));
      return Left(TelegramFailure('Unexpected error: ${e.toString()}'));
    }
  }

  // Progressive Telegram sticker download with UI feedback
  @override
  Future<Either<Failure, Map<String, dynamic>>> downloadTelegramStickersWithProgress(
    String url, {
    required Function(int currentIndex, int totalStickers, String currentUrl, List<String> downloadedFiles, List<Map<String, dynamic>> allStickers) onProgress,
  }) async {
    try {
      if (!_telegramApiService.isValidTelegramUrl(url)) {
        return const Left(ValidationFailure(AppConstants.invalidTelegramUrlMessage));
      }
      
      final packName = _telegramApiService.extractPackNameFromUrl(url);
      if (packName == null) {
        return const Left(ValidationFailure('Could not extract pack name from URL'));
      }
      
      // Get sticker pack info from Telegram
      final stickerPackInfo = await _telegramApiService.getStickerPackInfo(packName);
      final allStickers = stickerPackInfo['stickers'] as List<dynamic>?;
      
      if (allStickers == null || allStickers.isEmpty) {
        return const Left(TelegramFailure('No stickers found in this Telegram pack.'));
      }
      
      // Limit to WhatsApp maximum and prepare sticker data
      final stickersToProcess = allStickers.take(AppConstants.maxStickersInPack).toList();
      final stickerDataList = <Map<String, dynamic>>[];
      
      for (int i = 0; i < stickersToProcess.length; i++) {
        final stickerInfo = stickersToProcess[i];
        final stickerData = {
          'index': i,
          'file_id': stickerInfo['file_id'] as String,
          'emoji': stickerInfo['emoji'] as String? ?? '😀',
          'is_animated': stickerInfo['is_animated'] == true,
          'is_video': stickerInfo['is_video'] == true,
          'status': 'pending', // pending, downloading, processing, completed
          'progress': 0.0,
          'local_path': null,
          'selected': i < AppConstants.maxStickersInPack, // First 30 selected by default
        };
        stickerDataList.add(stickerData);
      }
      
      // Create downloads directory
      final appDir = await getApplicationDocumentsDirectory();
      final downloadDir = Directory('${appDir.path}/telegram_downloads');
      if (!await downloadDir.exists()) {
        await downloadDir.create(recursive: true);
      }
      
      final downloadedFiles = <String>[];
      
      // Download each sticker with progress updates
      for (int i = 0; i < stickerDataList.length; i++) {
        final stickerData = stickerDataList[i];
        
        try {
          // Update status to downloading
          stickerData['status'] = 'downloading';
          onProgress(i, stickerDataList.length, stickerData['file_id'], downloadedFiles, stickerDataList);
          
          final fileId = stickerData['file_id'] as String;
          final isAnimated = stickerData['is_animated'] as bool;
          final isVideo = stickerData['is_video'] as bool;
          
          // Determine file extension based on sticker type
          String fileName;
          if (isVideo) {
            fileName = '${i}_telegram_sticker.webm';
          } else if (isAnimated) {
            fileName = '${i}_telegram_sticker.webp'; // Animated WebP
          } else {
            fileName = '${i}_telegram_sticker.webp'; // Static WebP
          }
          
          final outputPath = '${downloadDir.path}/$fileName';
          
          // Download sticker file
          final stickerFile = await _telegramApiService.downloadSticker(
            fileId: fileId,
            fileName: fileName,
          );
          
          // Move to our expected location if different
          if (stickerFile.path != outputPath) {
            await stickerFile.copy(outputPath);
            await stickerFile.delete();
          }
          
          final downloadedFile = File(outputPath);
          if (await downloadedFile.exists()) {
            stickerData['local_path'] = outputPath;
            stickerData['status'] = 'downloaded';
            stickerData['progress'] = 0.5; // 50% - download complete
            
            // Update progress after download
            onProgress(i, stickerDataList.length, 'Downloaded', downloadedFiles, stickerDataList);
            
            // Convert WebM to WebP if needed
            if (isVideo && outputPath.endsWith('.webm')) {
              try {
                print('🔄 Starting WebM to WebP conversion for sticker ${i + 1}');
                stickerData['status'] = 'converting';
                onProgress(i, stickerDataList.length, 'Converting...', downloadedFiles, stickerDataList);
                
                // Process the downloaded WebM file to convert to WebP
                final processedBytes = await _imageProcessingService.processImageForWhatsApp(
                  outputPath,
                );
                
                // Create new WebP path
                final webpPath = outputPath.replaceAll('.webm', '.webp');
                await File(webpPath).writeAsBytes(processedBytes);
                
                // Delete original WebM file
                await downloadedFile.delete();
                
                // Update with converted path
                stickerData['local_path'] = webpPath;
                stickerData['status'] = 'completed';
                stickerData['progress'] = 1.0;
                downloadedFiles.add(webpPath);
                
                print('✅ Successfully converted WebM to WebP: $webpPath');
                print('📊 Converted file size: ${(processedBytes.length / 1024).toStringAsFixed(1)}KB');
              } catch (e) {
                print('❌ Failed to convert WebM to WebP: $e');
                stickerData['status'] = 'error';
                stickerData['error'] = 'Conversion failed: $e';
              }
            } else {
              // For non-WebM files, just mark as completed
              stickerData['status'] = 'completed';
              stickerData['progress'] = 1.0;
              downloadedFiles.add(outputPath);
            }
            
            // Update progress after processing
            onProgress(i + 1, stickerDataList.length, '', downloadedFiles, stickerDataList);
          } else {
            stickerData['status'] = 'error';
            print('Failed to download sticker ${i + 1}');
          }
          
        } catch (e) {
          stickerData['status'] = 'error';
          print('Error downloading sticker ${i + 1}: $e');
          // Continue with next sticker even if one fails
        }
        
        // Small delay between downloads to prevent overwhelming the API
        await Future.delayed(const Duration(milliseconds: 100));
      }
      
      // Filter out only successfully converted files (should be .webp)
      final successfulFiles = stickerDataList
          .where((data) => data['status'] == 'completed' && data['local_path'] != null)
          .map((data) => File(data['local_path'] as String))
          .where((file) => file.path.endsWith('.webp')) // Only include converted WebP files
          .toList();
      
      if (successfulFiles.isEmpty) {
        return const Left(TelegramFailure('Failed to download any stickers from the pack'));
      }
      
      return Right({
        'files': successfulFiles,
        'pack_name': stickerPackInfo['name'] ?? packName,
        'title': stickerPackInfo['title'] ?? packName,
        'stickers': stickerDataList,
      });
      
    } on TelegramException catch (e) {
      return Left(TelegramFailure(e.message));
    } on NetworkException catch (e) {
      return Left(NetworkFailure(e.message));
    } catch (e) {
      return Left(TelegramFailure('Unexpected error: ${e.toString()}'));
    }
  }

  // Fetch only Telegram pack metadata and sticker file IDs
  Future<Either<Failure, Map<String, dynamic>>> fetchTelegramPackMetadata(String url) async {
    try {
      if (!_telegramApiService.isValidTelegramUrl(url)) {
        return const Left(ValidationFailure('Invalid Telegram sticker pack URL'));
      }
      final packName = _telegramApiService.extractPackNameFromUrl(url);
      if (packName == null) {
        return const Left(ValidationFailure('Could not extract pack name from URL'));
      }
      final stickerPackInfo = await _telegramApiService.getStickerPackInfo(packName);
      final stickers = stickerPackInfo['stickers'] as List<dynamic>?;
      if (stickers == null || stickers.isEmpty) {
        return const Left(TelegramFailure('No stickers found in this Telegram pack.'));
      }
      // Return metadata: name, publisher, sticker file IDs, emojis, etc.
      return Right({
        'name': stickerPackInfo['name'] ?? packName,
        'title': stickerPackInfo['title'] ?? packName,
        'stickers': stickers,
      });
    } catch (e) {
      return Left(TelegramFailure('Failed to fetch Telegram pack metadata: ${e.toString()}'));
    }
  }
  
  @override
  Future<Either<Failure, StickerPackEntity>> processTelegramUrl({
    required String url,
    String? customPackName,
    String? customPublisher,
  }) async {
    print('=== UNIQUE_DEBUG: ENTERED Telegram Sticker Repository ===');
    try {
      _updateProgress(_currentState.copyWith(
        status: ProcessingStatus.processing,
      ));
      // Validate URL
      if (!_telegramApiService.isValidTelegramUrl(url)) {
        return const Left(ValidationFailure('Invalid Telegram sticker pack URL'));
      }
      // Extract pack name from URL
      final packName = _telegramApiService.extractPackNameFromUrl(url);
      if (packName == null) {
        return const Left(ValidationFailure('Could not extract pack name from URL'));
      }
      // Get sticker pack info from Telegram (real API call)
      final stickerPackInfo = await _telegramApiService.getStickerPackInfo(packName);
      final stickers = stickerPackInfo['stickers'] as List<dynamic>?;
      if (stickers == null || stickers.isEmpty) {
        return const Left(TelegramFailure('No stickers found in this Telegram pack.'));
      }
      _updateProgress(_currentState.copyWith(
        totalFiles: stickers.length,
      ));
      // Create output directory
      final outputDir = await _imageProcessingService.getOutputDirectory();
      final packDir = '$outputDir/telegram_${DateTime.now().millisecondsSinceEpoch}';
      await Directory(packDir).create(recursive: true);
      // Download and process stickers
      final processedStickers = <StickerModel>[];
      for (int i = 0; i < stickers.length; i++) {
        final stickerInfo = stickers[i];
        try {
          final fileId = stickerInfo['file_id'] as String;
          final emoji = stickerInfo['emoji'] as String? ?? '😀';
          final fileName = '${i}_telegram_sticker.webp';
          // Download sticker file
          final stickerFile = await _telegramApiService.downloadSticker(
            fileId: fileId,
            fileName: fileName,
          );
          // Process sticker for WhatsApp
          final processedBytes = await _imageProcessingService.processImageForWhatsApp(
            stickerFile.path,
          );
          final outputPath = '$packDir/$fileName';
          await File(outputPath).writeAsBytes(processedBytes);
          final fileSizeKB = (processedBytes.length / 1024).round();
          final dimensions = await _imageProcessingService.getImageDimensions(stickerFile.path);
          final sticker = StickerModel(
            imagePath: outputPath,
            emojis: [emoji],
            fileSizeKB: fileSizeKB,
            width: dimensions['width']!,
            height: dimensions['height']!,
          );
          processedStickers.add(sticker);
          _updateProgress(_currentState.copyWith(
            completedFiles: _currentState.completedFiles + 1,
          ));
        } catch (e) {
          _updateProgress(_currentState.copyWith(
            errorFiles: _currentState.errorFiles + 1,
          ));
          continue;
        }
      }
      if (processedStickers.isEmpty) {
        return const Left(TelegramFailure('No stickers could be downloaded or processed.'));
      }
      // Create tray image directly in the final sticker pack directory
      final trayImageBytes = await File(processedStickers.first.imagePath).readAsBytes();
      final compressedTrayBytes = await _imageProcessingService.createTrayImage(trayImageBytes);
      final trayPath = p.join(packDir, 'tray.png');
      await File(trayPath).writeAsBytes(compressedTrayBytes, flush: true);
      // Create sticker pack
      final pack = StickerPackModel(
        identifier: 'telegram_$packName',
        name: customPackName ?? packName,
        publisher: customPublisher ?? 'Telegram',
        trayImagePath: trayPath,
        stickers: processedStickers,
        animated: false, // TODO: Detect animated stickers and set this flag
      );
      _updateProgress(_currentState.copyWith(
        status: ProcessingStatus.completed,
      ));
      return Right(pack.toEntity());
    } on TelegramException catch (e) {
      _updateProgress(_currentState.copyWith(
        status: ProcessingStatus.error,
        error: e.message,
      ));
      return Left(TelegramFailure(e.message));
    } on NetworkException catch (e) {
      return Left(NetworkFailure(e.message));
    } catch (e) {
      _updateProgress(_currentState.copyWith(
        status: ProcessingStatus.error,
        error: e.toString(),
      ));
      return Left(TelegramFailure('Unexpected error:  ${e.toString()}'));
    }
  }
  
  @override
  Future<Either<Failure, bool>> addToWhatsApp(StickerPackEntity pack) async {
    try {
      final packModel = StickerPackModel.fromEntity(pack);
      
      // Validate pack
      await _whatsAppService.validateStickerPack(packModel);
      
      // Add to WhatsApp
      final result = await _whatsAppService.addStickerPackToWhatsApp(packModel);
      
      return Right(result);
      
    } on WhatsAppException catch (e) {
      return Left(WhatsAppFailure(e.message));
    } on ValidationException catch (e) {
      return Left(ValidationFailure(e.message));
    } catch (e) {
      return Left(WhatsAppFailure('Unexpected error: ${e.toString()}'));
    }
  }
  
  @override
  Future<Either<Failure, bool>> isWhatsAppInstalled() async {
    try {
      final isInstalled = await _whatsAppService.isWhatsAppInstalled();
      return Right(isInstalled);
    } on WhatsAppException catch (e) {
      return Left(WhatsAppFailure(e.message));
    } catch (e) {
      return Left(WhatsAppFailure('Failed to check WhatsApp: ${e.toString()}'));
    }
  }
  
  @override
  Stream<ProcessingState> getProcessingProgress() {
    return _progressController.stream;
  }
  
  @override
  Future<Either<Failure, List<String>>> validateFiles(List<File> files) async {
    try {
      final validFiles = <String>[];
      final errors = <String>[];
      
      print('Validating ${files.length} files');
      
      for (final file in files) {
        print('Validating file: ${file.path}');
        
        if (!await file.exists()) {
          print('File does not exist: ${file.path}');
          errors.add('File does not exist: ${file.path}');
          continue;
        }
        
        // Check file size first
        final fileSize = await file.length();
        print('File size: $fileSize bytes');
        
        if (fileSize == 0) {
          print('File is empty: ${file.path}');
          errors.add('File is empty: ${file.path}');
          continue;
        }
        
        if (fileSize > 10 * 1024 * 1024) { // 10MB limit for input files
          print('File too large: ${file.path}');
          errors.add('File too large: ${file.path}');
          continue;
        }
        
        // Check file format
        if (!_imageProcessingService.isValidImageFormat(file.path)) {
          print('Unsupported format: ${file.path}');
          errors.add('Unsupported format: ${file.path}');
          continue;
        }
        
        // Validate that the file is actually a valid image
        final isValidImage = await _imageProcessingService.isImageValid(file.path);
        if (!isValidImage) {
          print('Invalid image data: ${file.path}');
          errors.add('Invalid or corrupted image: ${file.path}');
          continue;
        }
        
        print('File validation passed: ${file.path}');
        validFiles.add(file.path);
      }
      
      print('Validation complete. Valid files: ${validFiles.length}, Errors: ${errors.length}');
      
      if (validFiles.isEmpty) {
        final errorMessage = errors.isNotEmpty 
            ? 'No valid files found. Issues: ${errors.join(', ')}'
            : 'No valid files found';
        return Left(ValidationFailure(errorMessage));
      }
      
      if (errors.isNotEmpty) {
        print('Some files had issues: ${errors.join(', ')}');
      }
      
      return Right(validFiles);
      
    } catch (e) {
      print('File validation error: ${e.toString()}');
      return Left(ValidationFailure('File validation failed: ${e.toString()}'));
    }
  }
  
  @override
  Future<Either<Failure, String>> extractZipFile(File zipFile) async {
    try {
      print('Starting ZIP extraction for: ${zipFile.path}');
      
      final bytes = await zipFile.readAsBytes();
      print('Read ZIP file bytes: ${bytes.length}');
      
      final archive = ZipDecoder().decodeBytes(bytes);
      print('ZIP archive contains ${archive.length} files');
      
      final outputDir = await _imageProcessingService.getOutputDirectory();
      final extractDir = '$outputDir/extracted_${DateTime.now().millisecondsSinceEpoch}';
      
      await Directory(extractDir).create(recursive: true);
      print('Created extraction directory: $extractDir');
      
      int extractedCount = 0;
      const maxImages = 30; // Limit to 30 images maximum
      
      for (final file in archive) {
        // Stop if we've reached the limit
        if (extractedCount >= maxImages) {
          print('Reached maximum limit of $maxImages images, stopping extraction');
          break;
        }
        
        if (file.isFile) {
          // Clean the file name to avoid path issues
          final fileName = file.name.split('/').last.split('\\').last;
          
          // Skip hidden files and system files
          if (fileName.startsWith('.') || fileName.startsWith('__')) {
            continue;
          }
          
          // Check if it's a supported image format
          final extension = fileName.toLowerCase().split('.').last;
          if (!['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].contains(extension)) {
            print('Skipping unsupported file: $fileName');
            continue;
          }
          
          final outputPath = '$extractDir/$fileName';
          print('Extracting: $fileName to $outputPath (${extractedCount + 1}/$maxImages)');
          
          try {
            final outputFile = File(outputPath);
            
            // Ensure parent directory exists
            await outputFile.parent.create(recursive: true);
            
            // Write file content
            final content = file.content as List<int>;
            await outputFile.writeAsBytes(content);
            
            // Verify the file was written successfully
            if (await outputFile.exists()) {
              final size = await outputFile.length();
              print('Successfully extracted: $fileName (${size} bytes)');
              
              // For WebP files, do additional validation since they're causing issues
              if (extension == 'webp') {
                if (size > 100) { // Basic size check
                  // Read the file back and check the header
                  final extractedBytes = await outputFile.readAsBytes();
                  if (extractedBytes.length >= 12) {
                    final headerHex = extractedBytes.sublist(0, 12).map((b) => b.toRadixString(16).padLeft(2, '0')).join(' ');
                    print('WebP file header: $headerHex');
                    
                    // Check for valid WebP signature
                    try {
                      final riffSig = String.fromCharCodes(extractedBytes.sublist(0, 4));
                      final webpSig = String.fromCharCodes(extractedBytes.sublist(8, 12));
                      
                      if (riffSig == 'RIFF' && webpSig == 'WEBP') {
                        extractedCount++;
                        print('WebP file with valid header accepted: $fileName');
                      } else {
                        print('WebP file has invalid header, may be corrupted: $fileName');
                        // Still accept it for now, but log the issue
                        extractedCount++;
                        print('WebP file accepted despite header issues: $fileName');
                      }
                    } catch (e) {
                      print('Error checking WebP header: $e');
                      extractedCount++;
                      print('WebP file accepted with header check error: $fileName');
                    }
                  } else {
                    extractedCount++;
                    print('WebP file too small for header check, but accepted: $fileName');
                  }
                } else {
                  print('WebP file too small, skipping: $fileName');
                  await outputFile.delete();
                }
              } else {
                // For other formats, verify the image can be decoded
                final isValid = await _imageProcessingService.isImageValid(outputPath);
                if (isValid) {
                  extractedCount++;
                  print('Image validation passed for: $fileName');
                } else {
                  print('Image validation failed for: $fileName');
                  await outputFile.delete();
                }
              }
            } else {
              print('Failed to create file: $fileName');
            }
          } catch (e) {
            print('Error extracting file $fileName: ${e.toString()}');
          }
        }
      }
      
      print('Successfully extracted $extractedCount valid image files (max: $maxImages)');
      
      if (extractedCount == 0) {
        return Left(ProcessingFailure('No valid image files found in ZIP archive'));
      }
      
      // Check if we hit the limit and inform the user
      final totalImageFiles = archive.where((file) => 
        file.isFile && 
        ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].contains(
          file.name.split('/').last.split('\\').last.toLowerCase().split('.').last
        )
      ).length;
      
      if (totalImageFiles > maxImages) {
        print('ZIP contained $totalImageFiles images, but only $maxImages were extracted due to limit');
      }
      
      return Right('$extractDir|$extractedCount|$totalImageFiles'); // Pass extra info for user notification
      
    } catch (e) {
      print('ZIP extraction error: ${e.toString()}');
      return Left(ProcessingFailure('Failed to extract ZIP file: ${e.toString()}'));
    }
  }
  
  void _updateProgress(ProcessingState newState) {
    _currentState = newState;
    _progressController.add(_currentState);
  }
  
  void _updateFileProgress(String filePath, double progress) {
    final updatedProgress = Map<String, double>.from(_currentState.fileProgress);
    updatedProgress[filePath] = progress;
    
    _updateProgress(_currentState.copyWith(
      fileProgress: updatedProgress,
    ));
  }
  
  void dispose() {
    _progressController.close();
  }
}
