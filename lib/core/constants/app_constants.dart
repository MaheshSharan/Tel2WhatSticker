class AppConstants {
  // App Info
  static const String appName = 'Telegram to WhatsApp Stickers';
  static const String appVersion = '1.0.0';
  
  // WhatsApp Requirements
  static const int stickerSize = 512;
  static const int maxStickerSize = 512;
  static const int maxFileSizeKB = 100;
  static const int maxStaticFileSizeKB = 100;
  static const int maxAnimatedFileSizeKB = 500;
  static const int minStickersInPack = 3;
  static const int maxStickersInPack = 30;
  
  // File Extensions
  static const List<String> supportedImageFormats = [
    'jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'
  ];
  
  // Telegram API
  static const String telegramStickerBaseUrl = 'https://t.me/addstickers/';
  static const String telegramApiBaseUrl = 'https://api.telegram.org';
  
  // Processing
  static const Duration processingTimeout = Duration(minutes: 5);
  static const int maxConcurrentProcessing = 3;
  
  // Cache
  static const String cacheDirectory = 'sticker_cache';
  static const Duration cacheExpiry = Duration(days: 7);
  
  // Error Messages
  static const String networkErrorMessage = 'Please check your internet connection';
  static const String processingErrorMessage = 'Failed to process sticker';
  static const String whatsAppNotInstalledMessage = 'WhatsApp is not installed';
  static const String invalidTelegramUrlMessage = 'Invalid Telegram sticker pack URL';
  
  // Success Messages
  static const String stickerPackAddedMessage = 'Sticker pack added to WhatsApp successfully!';
  static const String processingCompleteMessage = 'All stickers processed successfully';
}
