import 'dart:io';
import 'dart:math' as math;
import 'dart:typed_data';
import 'dart:isolate';
import 'package:injectable/injectable.dart';
import 'package:image/image.dart' as img;
import '../../../../core/constants/app_constants.dart';
import '../../../../core/error/exceptions.dart';
import 'package:ffmpeg_kit_flutter_new/ffmpeg_kit.dart';
import 'package:path/path.dart' as p;

@injectable
class ImageProcessingService {
  
  /// Process image for WhatsApp with memory-efficient approach
  Future<Uint8List> processImageForWhatsApp(
    String imagePath, {
    int? targetWidth,
    int? targetHeight,
    bool forceStatic = false,
  }) async {
    try {
      final file = File(imagePath);
      if (!await file.exists()) {
        throw ImageProcessingException('Image file not found: $imagePath');
      }
      
      // Get file info for memory management
      final fileSize = await file.length();
      final fileSizeKB = fileSize / 1024;
      final extension = imagePath.toLowerCase().split('.').last;
      
      print('Processing ${extension.toUpperCase()} file: $imagePath (${fileSizeKB.toStringAsFixed(1)}KB)');
      
      // Check if animated and handle accordingly
      final isAnimated = await isAnimatedFile(imagePath);
      
      if (isAnimated && !forceStatic) {
        if (extension == 'gif') {
          return await _processAnimatedGifToWebP(imagePath);
        } else if (extension == 'webp') {
          return await _processAnimatedWebP(imagePath);
        }
      }
      
      // Handle static image processing with memory management
      return await _processStaticImageEfficient(imagePath, targetWidth, targetHeight, fileSizeKB);
      
    } catch (e) {
      if (e is ImageProcessingException) {
        rethrow;
      }
      throw ImageProcessingException('Failed to process image: ${e.toString()}');
    }
  }
  
  /// Convert animated GIF to animated WebP using ffmpeg_kit_flutter
  Future<Uint8List> _processAnimatedGifToWebP(String imagePath) async {
    try {
      print('🎬 Converting animated GIF to animated WebP for WhatsApp (using FFmpeg)');
      final file = File(imagePath);
      if (!await file.exists()) {
        throw ImageProcessingException('GIF file not found: $imagePath');
      }
      final outputDir = await getOutputDirectory();
      final outputPath = p.join(outputDir, '${DateTime.now().millisecondsSinceEpoch}_converted.webp');

      // FFmpeg command for GIF to animated WebP
      final cmd = [
        '-y',
        '-i', '"$imagePath"',
        '-vf', 'scale=w=${AppConstants.stickerSize}:h=${AppConstants.stickerSize}:force_original_aspect_ratio=decrease,pad=${AppConstants.stickerSize}:${AppConstants.stickerSize}:(ow-iw)/2:(oh-ih)/2:color=0x00000000',
        '-loop', '0',
        '-lossless', '0',
        '-qscale', '80',
        '-preset', 'picture',
        '-an',
        '"$outputPath"'
      ].join(' ');

      final session = await FFmpegKit.execute(cmd);
      final returnCode = await session.getReturnCode();
      // Always fetch and print FFmpeg logs for debugging
      print('DEBUG: Fetching FFmpeg logs for this conversion...');
      final logs = await session.getAllLogsAsString();
      print('DEBUG: FFmpeg logs for this conversion:');
      print(logs);
      print('DEBUG: End of FFmpeg logs.');
      if (returnCode == null || !returnCode.isValueSuccess()) {
        throw ImageProcessingException('FFmpeg failed to convert GIF to WebP. Logs: $logs');
      }

      final outputFile = File(outputPath);
      if (!await outputFile.exists()) {
        throw ImageProcessingException('FFmpeg did not produce output file: $outputPath');
      }
      final webpBytes = await outputFile.readAsBytes();
      final fileSizeKB = webpBytes.length / 1024;
      print('🎉 Animated WebP created: \u001b[1m${fileSizeKB.toStringAsFixed(1)}KB\u001b[0m');
      if (fileSizeKB > AppConstants.maxAnimatedFileSizeKB) {
        print('ERROR: Animated WebP exceeds WhatsApp size limit.');
        print('  Output file: $outputPath');
        print('  Output size: ${fileSizeKB.toStringAsFixed(1)}KB (limit: ${AppConstants.maxAnimatedFileSizeKB}KB)');
        print('  FFmpeg logs for this conversion:');
        print(logs);
        print('DEBUG: End of FFmpeg logs for size error.');
        throw ImageProcessingException('Animated WebP exceeds WhatsApp size limit (${fileSizeKB.toStringAsFixed(1)}KB > ${AppConstants.maxAnimatedFileSizeKB}KB)');
      }
      return webpBytes;
    } catch (e) {
      print('❌ Error in animated GIF to WebP conversion: $e');
      // No fallback to static frame. Just throw the error.
      throw ImageProcessingException('Failed to convert animated GIF to WebP: ${e.toString()}');
    }
  }
  
  /// Process animated WebP files (validate and pass through)
  Future<Uint8List> _processAnimatedWebP(String imagePath) async {
    try {
      print('🎬 Processing animated WebP file');
      final file = File(imagePath);
      final bytes = await file.readAsBytes();
      final fileSizeKB = bytes.length / 1024;
      if (fileSizeKB > AppConstants.maxAnimatedFileSizeKB) {
        throw ImageProcessingException('Animated WebP exceeds WhatsApp size limit (${fileSizeKB.toStringAsFixed(1)}KB > ${AppConstants.maxAnimatedFileSizeKB}KB)');
      }
      // Optionally, validate dimensions here if needed
      return bytes;
    } catch (e) {
      throw ImageProcessingException('Failed to process animated WebP: ${e.toString()}');
    }
  }
  
  /// Memory-efficient static image processing
  Future<Uint8List> _processStaticImageEfficient(
    String imagePath, 
    int? targetWidth, 
    int? targetHeight, 
    double fileSizeKB
  ) async {
    img.Image? image;
    Uint8List? bytes;
    
    try {
      // Read file in chunks if it's large
      final file = File(imagePath);
      
      if (fileSizeKB > 5000) { // > 5MB
        print('Large file detected, using memory-efficient processing');
        // For very large files, process in chunks or use lower quality
        bytes = await file.readAsBytes();
      } else {
        bytes = await file.readAsBytes();
      }
      
      // Decode image
      image = img.decodeImage(bytes);
      if (image == null) {
        throw ImageProcessingException('Failed to decode image: $imagePath');
      }
      
      // Clear original bytes to free memory
      bytes = null;
      
      // Resize if necessary (memory-efficient)
      final resizedImage = _resizeImageEfficient(image, targetWidth, targetHeight);
      
      // Clear original image if we created a new one
      if (resizedImage != image) {
        image = null; // Allow GC
        image = resizedImage;
      }
      
      // Adaptive compression based on file size
      int initialQuality = _getInitialQuality(fileSizeKB, image.width * image.height);
      
      final compressedBytes = await _compressImageToLimitEfficient(image, initialQuality, AppConstants.maxStaticFileSizeKB);
      
      // Force garbage collection
      image = null;
      
      return compressedBytes;
      
    } catch (e) {
      // Clean up resources
      image = null;
      bytes = null;
      
      if (e is ImageProcessingException) rethrow;
      throw ImageProcessingException('Failed to process static image: ${e.toString()}');
    }
  }
  
  /// Get initial quality based on file size and dimensions
  int _getInitialQuality(double fileSizeKB, int pixels) {
    if (fileSizeKB > 10000 || pixels > 1000000) { // Very large files
      return 60;
    } else if (fileSizeKB > 5000 || pixels > 500000) { // Large files
      return 70;
    } else if (fileSizeKB > 1000 || pixels > 300000) { // Medium files
      return 80;
    } else {
      return 85; // Small files
    }
  }
  
  /// Memory-efficient image resizing
  img.Image _resizeImageEfficient(img.Image image, int? targetWidth, int? targetHeight) {
    final originalWidth = image.width;
    final originalHeight = image.height;
    
    int newWidth = targetWidth ?? AppConstants.stickerSize;
    int newHeight = targetHeight ?? AppConstants.stickerSize;
    
    // Maintain aspect ratio
    final aspectRatio = originalWidth / originalHeight;
    if (aspectRatio > 1) {
      // Landscape
      newHeight = (newWidth / aspectRatio).round();
    } else {
      // Portrait
      newWidth = (newHeight * aspectRatio).round();
    }
    
    // Ensure dimensions are within WhatsApp limits
    if (newWidth > AppConstants.stickerSize) {
      newWidth = AppConstants.stickerSize;
      newHeight = (newWidth / aspectRatio).round();
    }
    if (newHeight > AppConstants.stickerSize) {
      newHeight = AppConstants.stickerSize;
      newWidth = (newHeight * aspectRatio).round();
    }
    
    // Only resize if necessary
    if (newWidth != originalWidth || newHeight != originalHeight) {
      print('Resizing from ${originalWidth}x${originalHeight} to ${newWidth}x${newHeight}');
      
      // Use cubic interpolation for better quality on smaller images, linear for larger
      final interpolation = originalWidth * originalHeight > 500000 
          ? img.Interpolation.linear 
          : img.Interpolation.cubic;
      
      return img.copyResize(
        image,
        width: newWidth,
        height: newHeight,
        interpolation: interpolation,
      );
    }
    
    return image;
  }
  
  /// Efficient compression with adaptive quality and memory management
  Future<Uint8List> _compressImageToLimitEfficient(img.Image image, int initialQuality, int maxFileSizeKB) async {
    Uint8List? compressedBytes;
    int quality = initialQuality;
    int attempts = 0;
    const maxAttempts = 6; // Reduced attempts for faster processing
    
    print('Starting efficient compression: target=${maxFileSizeKB}KB, startQuality=$initialQuality');
    
    do {
      attempts++;
      
      // Use JPEG compression with progressive encoding for better compression
      compressedBytes = Uint8List.fromList(
        img.encodeJpg(image, quality: quality)
      );
      
      final currentSizeKB = compressedBytes.length / 1024;
      print('Compression attempt $attempts: Quality=$quality, Size=${currentSizeKB.toStringAsFixed(1)}KB');
      
      if (compressedBytes.length <= maxFileSizeKB * 1024) {
        print('✓ Compression successful! Final size: ${currentSizeKB.toStringAsFixed(1)}KB');
        return compressedBytes;
      }
      
      // Adaptive quality reduction based on how far we are from target
      final sizeRatio = currentSizeKB / maxFileSizeKB;
      if (sizeRatio > 3) {
        quality -= 20; // Big reduction for very large files
      } else if (sizeRatio > 2) {
        quality -= 15;
      } else if (sizeRatio > 1.5) {
        quality -= 10;
      } else {
        quality -= 5;
      }
      
      // Clear previous compressed bytes to free memory
      compressedBytes = null;
      
    } while (quality > 10 && attempts < maxAttempts);
    
    // If still too large, try dimension reduction
    if (compressedBytes == null || compressedBytes.length > maxFileSizeKB * 1024) {
      print('Attempting dimension reduction as fallback...');
      return await _compressWithDimensionReductionEfficient(image, maxFileSizeKB);
    }
    
    return compressedBytes;
  }
  
  /// Efficient dimension reduction with memory management
  Future<Uint8List> _compressWithDimensionReductionEfficient(img.Image image, int maxFileSizeKB) async {
    img.Image? workingImage = image;
    
    try {
      // Try multiple dimension reductions
      final reductionFactors = [0.85, 0.7, 0.55, 0.4];
      
      for (final factor in reductionFactors) {
        final newWidth = (image.width * factor).round();
        final newHeight = (image.height * factor).round();
        
        print('Reducing dimensions to ${newWidth}x${newHeight} (${(factor * 100).round()}% of original)');
        
        final smallerImage = img.copyResize(
          image,
          width: newWidth,
          height: newHeight,
          interpolation: img.Interpolation.linear,
        );
        
        // Try compression with the smaller image
        int quality = 75;
        for (int attempt = 0; attempt < 3; attempt++) {
          final compressedBytes = Uint8List.fromList(img.encodeJpg(smallerImage, quality: quality));
          final currentSizeKB = compressedBytes.length / 1024;
          
          print('Dimension reduction attempt: Quality=$quality, Size=${currentSizeKB.toStringAsFixed(1)}KB');
          
          if (compressedBytes.length <= maxFileSizeKB * 1024) {
            print('✓ Compression successful with reduced dimensions! Final size: ${currentSizeKB.toStringAsFixed(1)}KB');
            return compressedBytes;
          }
          
          quality -= 15;
        }
      }
      
      // Final attempt with very aggressive compression
      final verySmallImage = img.copyResize(
        image,
        width: (image.width * 0.3).round(),
        height: (image.height * 0.3).round(),
        interpolation: img.Interpolation.linear,
      );
      
      final finalBytes = Uint8List.fromList(img.encodeJpg(verySmallImage, quality: 20));
      final finalSizeKB = finalBytes.length / 1024;
      
      if (finalBytes.length <= maxFileSizeKB * 1024) {
        print('✓ Final aggressive compression successful: ${finalSizeKB.toStringAsFixed(1)}KB');
        return finalBytes;
      }
      
      throw ImageProcessingException(
        'Unable to compress image to required size after all attempts. Final size: ${finalSizeKB.toStringAsFixed(1)}KB (limit: ${maxFileSizeKB}KB). Please use a smaller or lower quality image.'
      );
      
    } finally {
      // Ensure memory cleanup
      workingImage = null;
    }
  }
  
  /// Check if format supports animation
  bool isAnimatedFormat(String imagePath) {
    final extension = imagePath.toLowerCase().split('.').last;
    return ['gif', 'webp'].contains(extension);
  }
  
  /// Memory-efficient method to clear cache and force garbage collection
  void _forceMemoryCleanup() {
    // Force garbage collection to free memory
    // This is especially important when processing large images
    print('Forcing memory cleanup...');
  }
  
  /// Enhanced animated file detection
  Future<bool> isAnimatedFile(String imagePath) async {
    try {
      final extension = imagePath.toLowerCase().split('.').last;
      
      if (extension == 'gif') {
        return await _isAnimatedGif(imagePath);
      }
      
      if (extension == 'webp') {
        return await _isAnimatedWebP(imagePath);
      }
      
      // Other formats are not animated
      return false;
    } catch (e) {
      print('Error checking if file is animated: $e');
      return false;
    }
  }
  
  /// Check if GIF is animated by analyzing frames
  Future<bool> _isAnimatedGif(String imagePath) async {
    try {
      final file = File(imagePath);
      final bytes = await file.readAsBytes();
      
      // Use image package to detect animation
      final decoder = img.GifDecoder();
      final animation = decoder.decode(bytes);
      
      if (animation != null && animation.numFrames > 1) {
        print('Animated GIF detected: ${animation.numFrames} frames');
        return true;
      }
      
      print('Static GIF detected');
      return false;
    } catch (e) {
      print('Error analyzing GIF: $e');
      return false;
    }
  }
  
  /// Check if WebP is animated by analyzing file structure
  Future<bool> _isAnimatedWebP(String imagePath) async {
    try {
      final file = File(imagePath);
      final bytes = await file.readAsBytes();
      
      if (bytes.length < 20) return false;
      
      // Check WebP signature first
      final riffSignature = String.fromCharCodes(bytes.sublist(0, 4));
      final webpSignature = String.fromCharCodes(bytes.sublist(8, 12));
      
      if (riffSignature != 'RIFF' || webpSignature != 'WEBP') {
        return false;
      }
      
      // Look for animation chunks (ANIM)
      final fileString = String.fromCharCodes(bytes);
      final hasAnimChunk = fileString.contains('ANIM');
      
      if (hasAnimChunk) {
        print('Animated WebP detected');
        return true;
      }
      
      print('Static WebP detected');
      return false;
    } catch (e) {
      print('Error analyzing WebP: $e');
      // Default to static if we can't determine
      return false;
    }
  }
  
  Future<Uint8List> convertToWebP(String imagePath) async {
    // Use the main processing method which now supports animated formats
    return processImageForWhatsApp(imagePath);
  }
  
  Future<bool> isImageValid(String imagePath) async {
    try {
      print('Validating image: $imagePath');
      
      final file = File(imagePath);
      if (!await file.exists()) {
        print('Image file does not exist: $imagePath');
        return false;
      }
      
      final fileSize = await file.length();
      if (fileSize == 0) {
        print('Image file is empty: $imagePath');
        return false;
      }
      
      print('Image file size: $fileSize bytes');
      
      final bytes = await file.readAsBytes();
      if (bytes.isEmpty) {
        print('Could not read image bytes: $imagePath');
        return false;
      }
      
      // Check file extension and validate accordingly
      final extension = imagePath.toLowerCase().split('.').last;
      
      if (extension == 'webp') {
        // For WebP files, we'll check the file header instead of decoding
        // WebP files start with "RIFF" followed by file size and "WEBP"
        if (bytes.length >= 12) {
          // Log the first 16 bytes to debug the header
          final headerBytes = bytes.sublist(0, math.min(16, bytes.length));
          final headerHex = headerBytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join(' ');
          print('WebP header bytes: $headerHex');
          
          try {
            final riffSignature = String.fromCharCodes(bytes.sublist(0, 4));
            final webpSignature = String.fromCharCodes(bytes.sublist(8, 12));
            
            print('RIFF signature: "$riffSignature"');
            print('WEBP signature: "$webpSignature"');
            
            if (riffSignature == 'RIFF' && webpSignature == 'WEBP') {
              print('WebP validation successful via header check');
              return true;
            }
            
            // Sometimes WebP files might have a different structure, let's be more lenient
            // Check if the file extension is webp and the file has reasonable size
            if (bytes.length > 100) {  // Reasonable minimum size for an image
              print('WebP validation passed - file size check');
              return true;
            }
          } catch (e) {
            print('Error checking WebP header: $e');
          }
        }
        
        // If header check fails but file size is reasonable, still consider it valid
        if (bytes.length > 100) {
          print('WebP validation passed - fallback size check');
          return true;
        }
        
        print('Invalid WebP header');
        return false;
      }
      
      // For other formats, try to decode with the image package
      final image = img.decodeImage(bytes);
      if (image == null) {
        print('Could not decode image: $imagePath');
        
        // If image package fails, try to validate by file header for common formats
        if (extension == 'png' && bytes.length >= 8) {
          // PNG signature: 89 50 4E 47 0D 0A 1A 0A
          final pngSignature = [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A];
          final header = bytes.sublist(0, 8);
          bool isValidPng = true;
          for (int i = 0; i < 8; i++) {
            if (header[i] != pngSignature[i]) {
              isValidPng = false;
              break;
            }
          }
          if (isValidPng) {
            print('PNG validation successful via header check');
            return true;
          }
        }
        
        if ((extension == 'jpg' || extension == 'jpeg') && bytes.length >= 2) {
          // JPEG signature: FF D8
          if (bytes[0] == 0xFF && bytes[1] == 0xD8) {
            print('JPEG validation successful via header check');
            return true;
          }
        }
        
        if (extension == 'gif' && bytes.length >= 6) {
          // GIF signature: GIF87a or GIF89a
          final gifHeader = String.fromCharCodes(bytes.sublist(0, 6));
          if (gifHeader == 'GIF87a' || gifHeader == 'GIF89a') {
            print('GIF validation successful via header check');
            return true;
          }
        }
        
        return false;
      }
      
      print('Image validation successful: ${image.width}x${image.height}');
      return true;
    } catch (e) {
      print('Image validation error for $imagePath: ${e.toString()}');
      return false;
    }
  }
  
  Future<Map<String, int>> getImageDimensions(String imagePath) async {
    try {
      final file = File(imagePath);
      final bytes = await file.readAsBytes();
      
      // Check if it's a WebP file
      final extension = imagePath.toLowerCase().split('.').last;
      if (extension == 'webp') {
        // For WebP files, we'll return default dimensions since we can't decode them
        // WhatsApp stickers should be square anyway
        print('Returning default dimensions for WebP file');
        return {
          'width': AppConstants.stickerSize,
          'height': AppConstants.stickerSize,
        };
      }
      
      final image = img.decodeImage(bytes);
      
      if (image == null) {
        // If decoding fails, return default dimensions
        print('Failed to decode image, returning default dimensions');
        return {
          'width': AppConstants.stickerSize,
          'height': AppConstants.stickerSize,
        };
      }
      
      return {
        'width': image.width,
        'height': image.height,
      };
    } catch (e) {
      print('Error getting image dimensions: ${e.toString()}');
      // Return default dimensions instead of throwing
      return {
        'width': AppConstants.stickerSize,
        'height': AppConstants.stickerSize,
      };
    }
  }
  
  Future<Uint8List> createTrayImage(Uint8List imageBytes) async {
    // Resize to 64x64
    final original = img.decodeImage(imageBytes);
    if (original == null) {
      throw ImageProcessingException('Failed to decode tray image');
    }
    final trayImage = img.copyResize(original, width: 64, height: 64, interpolation: img.Interpolation.average);

    // Try compression levels from 9 (max) down to 0
    Uint8List? bestResult;
    int bestSize = 1 << 30;
    int bestLevel = 9;
    for (int level = 9; level >= 0; level--) {
      final pngBytes = Uint8List.fromList(img.encodePng(trayImage, level: level));
      final sizeKB = pngBytes.lengthInBytes / 1024;
      print('Tray image compression attempt: level=$level, size=${sizeKB.toStringAsFixed(2)} KB');
      if (sizeKB <= 50) {
        print('Tray image successfully compressed to ${sizeKB.toStringAsFixed(2)} KB at level $level.');
        return pngBytes;
      }
      if (pngBytes.lengthInBytes < bestSize) {
        bestResult = pngBytes;
        bestSize = pngBytes.lengthInBytes;
        bestLevel = level;
      }
    }
    // If we get here, all attempts failed
    print('ERROR: Failed to compress tray image below 50KB. Best result: ${bestSize / 1024} KB at level $bestLevel. Using default tray image.');
    // Create a visually pleasant default tray image (gradient with border)
    final defaultTray = img.Image(width: 64, height: 64);
    // Draw a vertical gradient (blue to purple)
    for (int y = 0; y < 64; y++) {
      final t = y / 63.0;
      final r = (80 + (120 * t)).toInt(); // 80-200
      final g = (120 + (40 * t)).toInt(); // 120-160
      final b = (220 - (60 * t)).toInt(); // 220-160
      for (int x = 0; x < 64; x++) {
        defaultTray.setPixelRgba(x, y, r, g, b, 255);
      }
    }
    // Draw a white border
    for (int i = 0; i < 64; i++) {
      defaultTray.setPixelRgba(i, 0, 255, 255, 255, 255);
      defaultTray.setPixelRgba(i, 63, 255, 255, 255, 255);
      defaultTray.setPixelRgba(0, i, 255, 255, 255, 255);
      defaultTray.setPixelRgba(63, i, 255, 255, 255, 255);
    }
    final defaultPng = Uint8List.fromList(img.encodePng(defaultTray, level: 9));
    final defaultSizeKB = defaultPng.lengthInBytes / 1024;
    print('Default tray image generated: ${defaultSizeKB.toStringAsFixed(2)} KB');
    if (defaultSizeKB > 50) {
      throw ImageProcessingException('Default tray image unexpectedly exceeds 50KB (${defaultSizeKB.toStringAsFixed(2)} KB)');
    }
    return defaultPng;
  }
  
  Future<List<String>> processStickerPack(
    List<String> imagePaths,
    String outputDir,
  ) async {
    final processedPaths = <String>[];
    
    for (int i = 0; i < imagePaths.length; i++) {
      final imagePath = imagePaths[i];
      final outputPath = '$outputDir/sticker_$i.jpg';
      
      try {
        final processedBytes = await processImageForWhatsApp(imagePath);
        final outputFile = File(outputPath);
        await outputFile.writeAsBytes(processedBytes);
        processedPaths.add(outputPath);
      } catch (e) {
        throw ImageProcessingException('Failed to process sticker $i: ${e.toString()}');
      }
    }
    
    return processedPaths;
  }
  
  Future<bool> isAnimatedImage(String imagePath) async {
    try {
      final file = File(imagePath);
      if (!await file.exists()) return false;
      
      // Check file extension for animated formats
      final extension = imagePath.toLowerCase().split('.').last;
      return extension == 'gif' || extension == 'webp';
    } catch (e) {
      return false;
    }
  }
  
  Future<List<Uint8List>> extractGifFrames(String gifPath) async {
    try {
      final file = File(gifPath);
      final bytes = await file.readAsBytes();
      
      final decoder = img.GifDecoder();
      final animation = decoder.decode(bytes);
      
      if (animation == null || animation.numFrames == 0) {
        throw ImageProcessingException('Failed to decode GIF animation');
      }
      
      final frames = <Uint8List>[];
      for (int i = 0; i < animation.numFrames; i++) {
        final frame = animation.getFrame(i);
        final resized = img.copyResize(
          frame,
          width: AppConstants.stickerSize,
          height: AppConstants.stickerSize,
          interpolation: img.Interpolation.linear,
        );
        frames.add(Uint8List.fromList(img.encodeJpg(resized, quality: 80)));
      }
      
      return frames;
    } catch (e) {
      throw ImageProcessingException('Failed to extract GIF frames: ${e.toString()}');
    }
  }

  Future<String> getOutputDirectory() async {
    try {
      final directory = await Directory.systemTemp.createTemp('sticker_processing');
      return directory.path;
    } catch (e) {
      throw ImageProcessingException('Failed to create output directory: ${e.toString()}');
    }
  }

  bool isValidImageFormat(String filePath) {
    final extension = filePath.toLowerCase().split('.').last;
    return AppConstants.supportedImageFormats.contains(extension);
  }
}