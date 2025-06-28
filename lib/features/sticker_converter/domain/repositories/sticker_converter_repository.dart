import 'package:dartz/dartz.dart';
import '../entities/sticker_pack_entity.dart';
import '../../../../core/error/failures.dart';
import 'dart:io';

abstract class StickerConverterRepository {
  Future<Either<Failure, StickerPackEntity>> processImages({
    required List<File> images,
    required String packName,
    required String publisher,
  });
  
  Future<Either<Failure, StickerPackEntity>> processTelegramUrl({
    required String url,
    String? customPackName,
    String? customPublisher,
  });
  
  Future<Either<Failure, bool>> addToWhatsApp(StickerPackEntity pack);
  
  Future<Either<Failure, bool>> isWhatsAppInstalled();
  
  Stream<ProcessingState> getProcessingProgress();
  
  Future<Either<Failure, List<String>>> validateFiles(List<File> files);
  
  Future<Either<Failure, String>> extractZipFile(File zipFile);
}
