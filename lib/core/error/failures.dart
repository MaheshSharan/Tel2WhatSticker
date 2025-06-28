abstract class Failure {
  const Failure(this.message);
  final String message;
}

class ServerFailure extends Failure {
  const ServerFailure(super.message);
}

class CacheFailure extends Failure {
  const CacheFailure(super.message);
}

class NetworkFailure extends Failure {
  const NetworkFailure(super.message);
}

class ValidationFailure extends Failure {
  const ValidationFailure(super.message);
}

class ProcessingFailure extends Failure {
  const ProcessingFailure(super.message);
}

class WhatsAppFailure extends Failure {
  const WhatsAppFailure(super.message);
}

class TelegramFailure extends Failure {
  const TelegramFailure(super.message);
}
