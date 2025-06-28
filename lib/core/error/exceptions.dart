class ServerException implements Exception {
  final String message;
  const ServerException(this.message);
}

class CacheException implements Exception {
  final String message;
  const CacheException(this.message);
}

class NetworkException implements Exception {
  final String message;
  const NetworkException(this.message);
}

class ValidationException implements Exception {
  final String message;
  const ValidationException(this.message);
}

class ProcessingException implements Exception {
  final String message;
  const ProcessingException(this.message);
}

class WhatsAppException implements Exception {
  final String message;
  const WhatsAppException(this.message);
}

class TelegramException implements Exception {
  final String message;
  const TelegramException(this.message);
}

class ImageProcessingException implements Exception {
  final String message;
  const ImageProcessingException(this.message);
}
