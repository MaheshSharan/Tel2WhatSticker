import 'package:dartz/dartz.dart';
import 'package:injectable/injectable.dart';
import '../entities/sticker_pack_entity.dart';
import '../repositories/sticker_converter_repository.dart';
import '../../../../core/error/failures.dart';
import '../../../../core/usecases/usecase.dart';
import 'dart:io';

@injectable
class ProcessImagesUseCase implements UseCase<StickerPackEntity, ProcessImagesParams> {
  final StickerConverterRepository repository;
  
  ProcessImagesUseCase(this.repository);
  
  @override
  Future<Either<Failure, StickerPackEntity>> call(ProcessImagesParams params) async {
    return await repository.processImages(
      images: params.images,
      packName: params.packName,
      publisher: params.publisher,
    );
  }
}

class ProcessImagesParams {
  final List<File> images;
  final String packName;
  final String publisher;
  
  ProcessImagesParams({
    required this.images,
    required this.packName,
    required this.publisher,
  });
}
