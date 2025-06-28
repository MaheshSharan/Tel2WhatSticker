import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entities/sticker_pack_entity.dart';

part 'sticker_converter_state.freezed.dart';

@freezed
class StickerConverterState with _$StickerConverterState {
  const factory StickerConverterState({
    @Default(false) bool isLoading,
    @Default(false) bool isWhatsAppInstalled,
    @Default(false) bool isProcessing,
    StickerPackEntity? currentPack,
    ProcessingState? processingProgress,
    List<String>? validatedFiles,
    String? extractedDirectory,
    String? error,
    String? successMessage,
  }) = _StickerConverterState;
  
  const factory StickerConverterState.initial() = StickerConverterInitialState;
  
  const factory StickerConverterState.loading() = StickerConverterLoadingState;
  
  const factory StickerConverterState.processing({
    required ProcessingState progress,
  }) = StickerConverterProcessingState;
  
  const factory StickerConverterState.processCompleted({
    required StickerPackEntity pack,
  }) = StickerConverterProcessCompletedState;
  
  const factory StickerConverterState.whatsAppCheckCompleted({
    required bool isInstalled,
  }) = StickerConverterWhatsAppCheckCompletedState;
  
  const factory StickerConverterState.addedToWhatsApp({
    required StickerPackEntity pack,
  }) = StickerConverterAddedToWhatsAppState;
  
  const factory StickerConverterState.filesValidated({
    required List<String> validFiles,
  }) = StickerConverterFilesValidatedState;
  
  const factory StickerConverterState.zipExtracted({
    required String directory,
    int? extractedCount,
    int? totalCount,
  }) = StickerConverterZipExtractedState;
  
  const factory StickerConverterState.error({
    required String message,
  }) = StickerConverterErrorState;
  
  const factory StickerConverterState.success({
    required String message,
    StickerPackEntity? pack,
  }) = StickerConverterSuccessState;
}
