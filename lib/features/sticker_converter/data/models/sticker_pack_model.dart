import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entities/sticker_pack_entity.dart';

part 'sticker_pack_model.freezed.dart';
part 'sticker_pack_model.g.dart';

@freezed
class StickerPackModel with _$StickerPackModel {
  const factory StickerPackModel({
    required String identifier,
    required String name,
    required String publisher,
    required String trayImagePath,
    required List<StickerModel> stickers,
    @Default(false) bool animated,
    @Default(false) bool isWhitelistedPublisher,
  }) = _StickerPackModel;
  
  factory StickerPackModel.fromJson(Map<String, dynamic> json) =>
      _$StickerPackModelFromJson(json);
  
  factory StickerPackModel.fromEntity(StickerPackEntity entity) {
    return StickerPackModel(
      identifier: entity.identifier,
      name: entity.name,
      publisher: entity.publisher,
      trayImagePath: entity.trayImagePath,
      stickers: entity.stickers.map((s) => StickerModel.fromEntity(s)).toList(),
      animated: entity.animated,
      isWhitelistedPublisher: entity.isWhitelistedPublisher,
    );
  }
}

extension StickerPackModelX on StickerPackModel {
  StickerPackEntity toEntity() {
    return StickerPackEntity(
      identifier: identifier,
      name: name,
      publisher: publisher,
      trayImagePath: trayImagePath,
      stickers: stickers.map((s) => s.toEntity()).toList(),
      animated: animated,
      isWhitelistedPublisher: isWhitelistedPublisher,
    );
  }
}

@freezed
class StickerModel with _$StickerModel {
  const factory StickerModel({
    required String imagePath,
    required List<String> emojis,
    @Default(0) int fileSizeKB,
    @Default(0) int width,
    @Default(0) int height,
  }) = _StickerModel;
  
  factory StickerModel.fromJson(Map<String, dynamic> json) =>
      _$StickerModelFromJson(json);
  
  factory StickerModel.fromEntity(StickerEntity entity) {
    return StickerModel(
      imagePath: entity.imagePath,
      emojis: entity.emojis,
      fileSizeKB: entity.fileSizeKB,
      width: entity.width,
      height: entity.height,
    );
  }
}

extension StickerModelX on StickerModel {
  StickerEntity toEntity() {
    return StickerEntity(
      imagePath: imagePath,
      emojis: emojis,
      fileSizeKB: fileSizeKB,
      width: width,
      height: height,
    );
  }
}
