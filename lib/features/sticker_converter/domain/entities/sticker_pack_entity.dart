import 'package:freezed_annotation/freezed_annotation.dart';

part 'sticker_pack_entity.freezed.dart';

@freezed
class StickerPackEntity with _$StickerPackEntity {
  const factory StickerPackEntity({
    required String identifier,
    required String name,
    required String publisher,
    required String trayImagePath,
    required List<StickerEntity> stickers,
    @Default(false) bool animated,
    @Default(false) bool isWhitelistedPublisher,
  }) = _StickerPackEntity;
}

@freezed
class StickerEntity with _$StickerEntity {
  const factory StickerEntity({
    required String imagePath,
    required List<String> emojis,
    @Default(0) int fileSizeKB,
    @Default(0) int width,
    @Default(0) int height,
  }) = _StickerEntity;
}

enum ProcessingStatus { idle, processing, completed, error }

@freezed
class ProcessingState with _$ProcessingState {
  const factory ProcessingState({
    @Default({}) Map<String, double> fileProgress,
    @Default(ProcessingStatus.idle) ProcessingStatus status,
    String? error,
    @Default(0) int totalFiles,
    @Default(0) int completedFiles,
    @Default(0) int errorFiles,
  }) = _ProcessingState;
}
