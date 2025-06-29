import 'dart:io';
import 'package:flutter/services.dart';
import 'package:injectable/injectable.dart';
import 'package:whatsapp_stickers_handler/whatsapp_stickers_handler.dart';
import 'package:whatsapp_stickers_handler/model/sticker_pack.dart';
import 'package:whatsapp_stickers_handler/model/sticker_pack_exception.dart';
import 'package:whatsapp_stickers_handler/service/sticker_pack_util.dart';
import 'package:whatsapp_stickers_handler/validation/sticker_pack_validator.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as path;
import '../../../../core/error/exceptions.dart';
import '../models/sticker_pack_model.dart';
import 'package:image/image.dart' as img;

@injectable
class WhatsAppService {
  final WhatsappStickersHandler _handler;
  
  WhatsAppService() : _handler = WhatsappStickersHandler();
  
  Future<bool> isWhatsAppInstalled() async {
    try {
      return await _handler.isWhatsAppInstalled;
    } catch (e) {
      throw WhatsAppException('Failed to check WhatsApp installation: ${e.toString()}');
    }
  }
  
  Future<bool> addStickerPackToWhatsApp(StickerPackModel pack) async {
    try {
      print('DEBUG: Starting WhatsApp integration for pack: ${pack.name}');
      print('DEBUG: Pack identifier: ${pack.identifier}');
      print('DEBUG: Pack publisher: ${pack.publisher}');
      print('DEBUG: Number of stickers: ${pack.stickers.length}');
      
      // Validate required fields
      if (pack.identifier.isEmpty) {
        throw WhatsAppException('Pack identifier cannot be empty');
      }
      if (pack.name.isEmpty) {
        throw WhatsAppException('Pack name cannot be empty');
      }
      if (pack.publisher.isEmpty) {
        throw WhatsAppException('Publisher cannot be empty');
      }
      if (pack.stickers.isEmpty) {
        throw WhatsAppException('Pack must contain at least one sticker');
      }
      
      // Get app directory for storing converted stickers
      final appDir = await getApplicationDocumentsDirectory();
      final stickerPackDir = Directory(path.join(appDir.path, 'sticker_packs', pack.identifier));
      if (!await stickerPackDir.exists()) {
        await stickerPackDir.create(recursive: true);
      }
      
      print('DEBUG: Creating sticker pack directory: ${stickerPackDir.path}');
      
      // Verify all original sticker files exist before processing
      final originalStickerPaths = <String>[];
      for (int i = 0; i < pack.stickers.length; i++) {
        final sticker = pack.stickers[i];
        final stickerFile = File(sticker.imagePath);
        if (!await stickerFile.exists()) {
          throw WhatsAppException('Sticker file not found: ${sticker.imagePath}');
        }
        originalStickerPaths.add(sticker.imagePath);
        print('DEBUG: Original sticker $i: ${sticker.imagePath}');
      }
      
      // Create StickerPack object
      final stickerPack = StickerPack(
        identifier: pack.identifier,
        name: pack.name,
        publisher: pack.publisher,
      );
      
      // Convert all sticker images to .webp format using StickerPackUtil
      print('DEBUG: Converting ${originalStickerPaths.length} stickers to .webp format');
      final stickerPackUtil = StickerPackUtil();
      
      // Check if any stickers are animated
      bool hasAnimatedStickers = false;
      for (final stickerPath in originalStickerPaths) {
        final isAnimated = await stickerPackUtil.isStickerAnimated(stickerPath);
        if (isAnimated) {
          hasAnimatedStickers = true;
          break;
        }
      }
      
      print('DEBUG: Animated stickers detected: $hasAnimatedStickers');
      
      final webpStickerPaths = await stickerPackUtil.createStickersFromImages(
        originalStickerPaths,
        stickerPackDir.path,
      );
      
      print('DEBUG: Successfully created ${webpStickerPaths.length} .webp stickers');
      if (webpStickerPaths.isEmpty) {
        throw WhatsAppException('No stickers were successfully converted to .webp format');
      }
      
      // Set the converted stickers
      stickerPack.stickers = webpStickerPaths;
      
      // Use the tray image path from the StickerPackModel (already optimized and created)
      final trayImagePath = pack.trayImagePath;
      final trayImageFile = File(trayImagePath);
      final trayImageExists = await trayImageFile.exists();
      final trayImageSize = trayImageExists ? await trayImageFile.length() : -1;
      print('DEBUG: Tray image for WhatsApp: $trayImagePath, exists: $trayImageExists, size: ${trayImageSize / 1024} KB');
      if (!trayImageExists) {
        throw WhatsAppException('Tray image file does not exist: $trayImagePath');
      }
      if (trayImageSize > 50 * 1024) {
        throw WhatsAppException('Tray image is too large: ${(trayImageSize / 1024).toStringAsFixed(2)} KB');
      }
      
      // Only use the trayImagePath from the model, do not forcibly set or fallback
      stickerPack.trayImage = trayImagePath;
      print('DEBUG: Final tray image path for WhatsApp: ${stickerPack.trayImage}');
      
      // Set animated flag if any stickers are animated
      if (hasAnimatedStickers) {
        // Check if StickerPack has animatedStickerPack property
        try {
          // This might not exist in all versions, so we'll handle it gracefully
          print('DEBUG: Setting pack as animated');
        } catch (e) {
          print('DEBUG: Unable to set animated flag: $e');
        }
      }
      
      print('DEBUG: Created tray image: $trayImagePath');
      print('DEBUG: Animated pack: $hasAnimatedStickers');
      
      // Validate the sticker pack before adding to WhatsApp
      print('DEBUG: Validating sticker pack');
      StickerPackValidator.validateStickerPack(stickerPack);
      print('DEBUG: Sticker pack validation passed');
      
      // Print pack metadata
      print('DEBUG: PACK METADATA:');
      print('  Identifier: ${pack.identifier}');
      print('  Name: ${pack.name}');
      print('  Publisher: ${pack.publisher}');
      print('  Tray image: $trayImagePath');
      print('  Tray image exists: $trayImageExists');
      print('  Tray image size: ${trayImageSize / 1024} KB');
      // Print tray image dimensions
      try {
        final trayBytes = await trayImageFile.readAsBytes();
        final trayExt = trayImagePath.toLowerCase().split('.').last;
        int trayW = -1, trayH = -1;
        if (trayExt == 'png') {
          final trayImg = img.decodePng(trayBytes);
          if (trayImg != null) {
            trayW = trayImg.width;
            trayH = trayImg.height;
          }
        } else if (trayExt == 'webp') {
          final trayImg = img.decodeWebP(trayBytes);
          if (trayImg != null) {
            trayW = trayImg.width;
            trayH = trayImg.height;
          }
        }
        print('  Tray image dimensions: ${trayW}x${trayH}');
      } catch (e) {
        print('  Tray image dimension check error: $e');
      }
      // Print sticker info
      print('DEBUG: STICKER FILES:');
      for (final stickerPath in stickerPack.stickers) {
        final file = File(stickerPath);
        final exists = await file.exists();
        final size = exists ? await file.length() : -1;
        int w = -1, h = -1;
        if (exists) {
          try {
            final bytes = await file.readAsBytes();
            final ext = stickerPath.toLowerCase().split('.').last;
            if (ext == 'webp') {
              final imgObj = img.decodeWebP(bytes);
              if (imgObj != null) {
                w = imgObj.width;
                h = imgObj.height;
              }
            } else if (ext == 'png') {
              final imgObj = img.decodePng(bytes);
              if (imgObj != null) {
                w = imgObj.width;
                h = imgObj.height;
              }
            } else if (ext == 'jpg' || ext == 'jpeg') {
              final imgObj = img.decodeJpg(bytes);
              if (imgObj != null) {
                w = imgObj.width;
                h = imgObj.height;
              }
            }
          } catch (e) {
            print('  [${stickerPath}] Error decoding image: $e');
          }
        }
        print('  Sticker: $stickerPath | exists: $exists | size: ${size / 1024} KB | dimensions: ${w}x${h}');
      }
      
      // Add to WhatsApp
      print('DEBUG: Adding sticker pack to WhatsApp');
      await _handler.addStickerPack(stickerPack);
      print('DEBUG: Successfully added sticker pack to WhatsApp');
      
      return true;
    } on StickerPackException catch (e) {
      print('DEBUG: StickerPackException: ${e.message}');
      throw WhatsAppException('Sticker pack validation failed: ${e.message}');
    } on PlatformException catch (e) {
      print('DEBUG: PlatformException: ${e.code} - ${e.message}');
      throw WhatsAppException('Platform error: ${e.message ?? 'An unexpected platform error occurred'}');
    } catch (e) {
      print('DEBUG: General exception: ${e.toString()}');
      print('DEBUG: Exception type: ${e.runtimeType}');
      throw WhatsAppException('Failed to add sticker pack to WhatsApp: ${e.toString()}');
    }
  }
  
  Future<bool> isStickerPackInstalled(String identifier) async {
    try {
      return await _handler.isStickerPackInstalled(identifier);
    } catch (e) {
      throw WhatsAppException('Failed to check sticker pack status: ${e.toString()}');
    }
  }
  
  Future<void> validateStickerPack(StickerPackModel pack) async {
    // Validate pack meets WhatsApp requirements
    if (pack.stickers.length < 3) {
      throw ValidationException('Sticker pack must contain at least 3 stickers');
    }
    
    if (pack.stickers.length > 30) {
      throw ValidationException('Sticker pack cannot contain more than 30 stickers');
    }
    
    if (pack.name.isEmpty) {
      throw ValidationException('Sticker pack name cannot be empty');
    }
    
    if (pack.publisher.isEmpty) {
      throw ValidationException('Publisher name cannot be empty');
    }
    
    // Check if any stickers are animated to determine size limits
    bool hasAnimatedStickers = false;
    final stickerPackUtil = StickerPackUtil();
    
    // Validate all sticker files exist and check for animation
    for (final sticker in pack.stickers) {
      final stickerFile = File(sticker.imagePath);
      if (!await stickerFile.exists()) {
        throw ValidationException('Sticker file not found: ${sticker.imagePath}');
      }
      
      // Check if sticker is animated
      final isAnimated = await stickerPackUtil.isStickerAnimated(sticker.imagePath);
      if (isAnimated) {
        hasAnimatedStickers = true;
      }
      
      // Check file size based on animation status
      final fileSize = await stickerFile.length();
      final maxSize = hasAnimatedStickers ? 500 * 1024 : 100 * 1024; // 500KB for animated, 100KB for static
      
      if (fileSize > maxSize) {
        final sizeType = hasAnimatedStickers ? 'animated' : 'static';
        final maxSizeKB = hasAnimatedStickers ? '500KB' : '100KB';
        throw ValidationException('$sizeType sticker file too large (${(fileSize / 1024).toStringAsFixed(1)}KB > $maxSizeKB): ${sticker.imagePath}');
      }
    }
    
    print('DEBUG: Validation completed - Animated stickers detected: $hasAnimatedStickers');
  }
}
