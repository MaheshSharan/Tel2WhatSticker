import 'dart:async';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:injectable/injectable.dart';
import '../../domain/usecases/process_images_usecase.dart';
import '../../domain/usecases/process_telegram_url_usecase.dart';
import '../../domain/usecases/add_to_whatsapp_usecase.dart';
import '../../domain/usecases/check_whatsapp_usecase.dart';
import '../../domain/repositories/sticker_converter_repository.dart';
import '../../../../core/usecases/usecase.dart';
import 'sticker_converter_event.dart';
import 'sticker_converter_state.dart';

@injectable
class StickerConverterBloc extends Bloc<StickerConverterEvent, StickerConverterState> {
  final ProcessImagesUseCase _processImagesUseCase;
  final ProcessTelegramUrlUseCase _processTelegramUrlUseCase;
  final AddToWhatsAppUseCase _addToWhatsAppUseCase;
  final CheckWhatsAppUseCase _checkWhatsAppUseCase;
  final StickerConverterRepository _repository;
  
  StreamSubscription? _progressSubscription;
  
  StickerConverterBloc(
    this._processImagesUseCase,
    this._processTelegramUrlUseCase,
    this._addToWhatsAppUseCase,
    this._checkWhatsAppUseCase,
    this._repository,
  ) : super(const StickerConverterState.initial()) {
    on<ProcessImagesEvent>(_onProcessImages);
    on<ProcessTelegramUrlEvent>(_onProcessTelegramUrl);
    on<AddToWhatsAppEvent>(_onAddToWhatsApp);
    on<CheckWhatsAppInstallationEvent>(_onCheckWhatsAppInstallation);
    on<ValidateFilesEvent>(_onValidateFiles);
    on<ExtractZipFileEvent>(_onExtractZipFile);
    on<ResetStateEvent>(_onResetState);
    on<UpdateProgressEvent>(_onUpdateProgress);
    
    // Listen to processing progress
    _progressSubscription = _repository.getProcessingProgress().listen(
      (progress) => add(StickerConverterEvent.updateProgress(progress: progress)),
    );
  }
  
  Future<void> _onProcessImages(
    ProcessImagesEvent event,
    Emitter<StickerConverterState> emit,
  ) async {
    emit(const StickerConverterState.loading());
    
    final result = await _processImagesUseCase(ProcessImagesParams(
      images: event.images,
      packName: event.packName,
      publisher: event.publisher,
    ));
    
    result.fold(
      (failure) => emit(StickerConverterState.error(
        message: failure.message,
      )),
      (pack) => emit(StickerConverterState.processCompleted(
        pack: pack,
      )),
    );
  }
  
  Future<void> _onProcessTelegramUrl(
    ProcessTelegramUrlEvent event,
    Emitter<StickerConverterState> emit,
  ) async {
    emit(const StickerConverterState.loading());
    
    final result = await _processTelegramUrlUseCase(ProcessTelegramUrlParams(
      url: event.url,
      customPackName: event.customPackName,
      customPublisher: event.customPublisher,
    ));
    
    result.fold(
      (failure) => emit(StickerConverterState.error(
        message: failure.message,
      )),
      (pack) => emit(StickerConverterState.processCompleted(
        pack: pack,
      )),
    );
  }
  
  Future<void> _onAddToWhatsApp(
    AddToWhatsAppEvent event,
    Emitter<StickerConverterState> emit,
  ) async {
    emit(const StickerConverterState.loading());
    
    final result = await _addToWhatsAppUseCase(AddToWhatsAppParams(
      pack: event.pack,
    ));
    
    result.fold(
      (failure) => emit(StickerConverterState.error(
        message: failure.message,
      )),
      (success) {
        if (success) {
          emit(StickerConverterState.addedToWhatsApp(
            pack: event.pack,
          ));
        } else {
          emit(const StickerConverterState.error(
            message: 'Failed to add sticker pack to WhatsApp',
          ));
        }
      },
    );
  }
  
  Future<void> _onCheckWhatsAppInstallation(
    CheckWhatsAppInstallationEvent event,
    Emitter<StickerConverterState> emit,
  ) async {
    final result = await _checkWhatsAppUseCase(const NoParams());
    
    result.fold(
      (failure) => emit(StickerConverterState.error(
        message: failure.message,
      )),
      (isInstalled) => emit(StickerConverterState.whatsAppCheckCompleted(
        isInstalled: isInstalled,
      )),
    );
  }
  
  Future<void> _onValidateFiles(
    ValidateFilesEvent event,
    Emitter<StickerConverterState> emit,
  ) async {
    emit(const StickerConverterState.loading());
    
    final result = await _repository.validateFiles(event.files);
    
    result.fold(
      (failure) => emit(StickerConverterState.error(
        message: failure.message,
      )),
      (validFiles) => emit(StickerConverterState.filesValidated(
        validFiles: validFiles,
      )),
    );
  }
  
  Future<void> _onExtractZipFile(
    ExtractZipFileEvent event,
    Emitter<StickerConverterState> emit,
  ) async {
    emit(const StickerConverterState.loading());
    
    final result = await _repository.extractZipFile(event.zipFile);
    
    result.fold(
      (failure) => emit(StickerConverterState.error(
        message: failure.message,
      )),
      (directoryInfo) {
        // Parse the directory info which contains: directory|extractedCount|totalCount
        final parts = directoryInfo.split('|');
        final directory = parts[0];
        final extractedCount = parts.length > 1 ? int.tryParse(parts[1]) : null;
        final totalCount = parts.length > 2 ? int.tryParse(parts[2]) : null;
        
        emit(StickerConverterState.zipExtracted(
          directory: directory,
          extractedCount: extractedCount,
          totalCount: totalCount,
        ));
      },
    );
  }
  
  void _onResetState(
    ResetStateEvent event,
    Emitter<StickerConverterState> emit,
  ) {
    emit(const StickerConverterState.initial());
  }
  
  void _onUpdateProgress(
    UpdateProgressEvent event,
    Emitter<StickerConverterState> emit,
  ) {
    emit(StickerConverterState.processing(
      progress: event.progress,
    ));
  }
  
  @override
  Future<void> close() {
    _progressSubscription?.cancel();
    return super.close();
  }
}
