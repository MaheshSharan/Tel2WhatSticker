import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entities/sticker_pack_entity.dart';
import 'dart:io';

part 'sticker_converter_event.freezed.dart';

@freezed
class StickerConverterEvent with _$StickerConverterEvent {
  const factory StickerConverterEvent.processImages({
    required List<File> images,
    required String packName,
    required String publisher,
  }) = ProcessImagesEvent;
  
  const factory StickerConverterEvent.processTelegramUrl({
    required String url,
    String? customPackName,
    String? customPublisher,
  }) = ProcessTelegramUrlEvent;
  
  const factory StickerConverterEvent.addToWhatsApp({
    required StickerPackEntity pack,
  }) = AddToWhatsAppEvent;
  
  const factory StickerConverterEvent.checkWhatsAppInstallation() = CheckWhatsAppInstallationEvent;
  
  const factory StickerConverterEvent.validateFiles({
    required List<File> files,
  }) = ValidateFilesEvent;
  
  const factory StickerConverterEvent.extractZipFile({
    required File zipFile,
  }) = ExtractZipFileEvent;
  
  const factory StickerConverterEvent.resetState() = ResetStateEvent;
  
  const factory StickerConverterEvent.updateProgress({
    required ProcessingState progress,
  }) = UpdateProgressEvent;
  
  const factory StickerConverterEvent.fetchTelegramPackMetadata({
    required String url,
  }) = FetchTelegramPackMetadataEvent;
  
  // New event for unified Telegram approach
  const factory StickerConverterEvent.downloadTelegramStickers({
    required String url,
  }) = DownloadTelegramStickersEvent;
}
