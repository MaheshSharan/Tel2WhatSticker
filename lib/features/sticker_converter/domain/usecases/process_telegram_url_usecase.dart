import 'package:dartz/dartz.dart';
import 'package:injectable/injectable.dart';
import '../entities/sticker_pack_entity.dart';
import '../repositories/sticker_converter_repository.dart';
import '../../../../core/error/failures.dart';
import '../../../../core/usecases/usecase.dart';

@injectable
class ProcessTelegramUrlUseCase implements UseCase<StickerPackEntity, ProcessTelegramUrlParams> {
  final StickerConverterRepository repository;
  
  ProcessTelegramUrlUseCase(this.repository);
  
  @override
  Future<Either<Failure, StickerPackEntity>> call(ProcessTelegramUrlParams params) async {
    return await repository.processTelegramUrl(
      url: params.url,
      customPackName: params.customPackName,
      customPublisher: params.customPublisher,
    );
  }
}

class ProcessTelegramUrlParams {
  final String url;
  final String? customPackName;
  final String? customPublisher;
  
  ProcessTelegramUrlParams({
    required this.url,
    this.customPackName,
    this.customPublisher,
  });
}
