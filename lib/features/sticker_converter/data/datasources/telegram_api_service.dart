import 'dart:io';
import 'package:dio/dio.dart';
import 'package:injectable/injectable.dart';
import 'package:path_provider/path_provider.dart';
import '../../../../core/error/exceptions.dart';
import '../../../../core/constants/app_constants.dart';

@injectable
class TelegramApiService {
  final Dio _dio;
  
  TelegramApiService(this._dio);
  
  Future<Map<String, dynamic>> getStickerPackInfo(String packName) async {
    try {
      final url = '${AppConstants.telegramApiBaseUrl}/bot${AppConstants.telegramBotToken}/getStickerSet';
      final response = await _dio.post(url, data: {
        'name': packName,
      });
      
      if (response.statusCode == 200 && response.data['ok'] == true) {
        return response.data['result'];
      } else {
        throw TelegramException('Failed to fetch sticker pack info');
      }
    } on DioException catch (e) {
      throw NetworkException('Network error: ${e.message}');
    } catch (e) {
      throw TelegramException('Telegram API error: ${e.toString()}');
    }
  }
  
  Future<File> downloadSticker({
    required String fileId,
    required String fileName,
  }) async {
    print('=== UNIQUE_DEBUG: ENTERED Telegram API Service ===');
    try {
      final fileResponse = await _dio.post(
        '${AppConstants.telegramApiBaseUrl}/bot${AppConstants.telegramBotToken}/getFile',
        data: {'file_id': fileId},
      );
      
      if (fileResponse.statusCode != 200 || fileResponse.data['ok'] != true) {
        throw TelegramException('Failed to get file path');
      }
      
      final filePath = fileResponse.data['result']['file_path'];
      final downloadUrl = 'https://api.telegram.org/file/bot${AppConstants.telegramBotToken}/$filePath';
      
      // Download file
      final appDir = await getApplicationDocumentsDirectory();
      final downloadDir = Directory('${appDir.path}/telegram_downloads');
      
      if (!await downloadDir.exists()) {
        await downloadDir.create(recursive: true);
      }
      
      final outputPath = '${downloadDir.path}/$fileName';
      await _dio.download(downloadUrl, outputPath);
      
      return File(outputPath);
    } on DioException catch (e) {
      throw NetworkException('Download failed: ${e.message}');
    } catch (e) {
      throw TelegramException('Failed to download sticker: ${e.toString()}');
    }
  }
  
  String? extractPackNameFromUrl(String url) {
    try {
      final uri = Uri.parse(url);
      
      // Handle different Telegram URL formats
      if (uri.host == 't.me' && uri.pathSegments.isNotEmpty) {
        if (uri.pathSegments[0] == 'addstickers' && uri.pathSegments.length > 1) {
          return uri.pathSegments[1];
        }
      }
      
      return null;
    } catch (e) {
      return null;
    }
  }
  
  bool isValidTelegramUrl(String url) {
    try {
      final uri = Uri.parse(url);
      return uri.host == 't.me' && 
             uri.pathSegments.isNotEmpty && 
             uri.pathSegments[0] == 'addstickers';
    } catch (e) {
      return false;
    }
  }
  
  // Note: This is a simplified implementation for demo purposes
  // In a production app, you would need:
  // 1. Proper Telegram Bot API token
  // 2. Handle rate limiting
  // 3. Implement proper authentication
  // 4. Handle different sticker formats (static/animated)
  // 5. Parse sticker metadata (emojis, etc.)
}
