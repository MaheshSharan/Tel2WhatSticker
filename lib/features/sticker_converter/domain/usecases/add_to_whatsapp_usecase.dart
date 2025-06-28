import 'package:dartz/dartz.dart';
import 'package:injectable/injectable.dart';
import '../entities/sticker_pack_entity.dart';
import '../repositories/sticker_converter_repository.dart';
import '../../../../core/error/failures.dart';
import '../../../../core/usecases/usecase.dart';

@injectable
class AddToWhatsAppUseCase implements UseCase<bool, AddToWhatsAppParams> {
  final StickerConverterRepository repository;
  
  AddToWhatsAppUseCase(this.repository);
  
  @override
  Future<Either<Failure, bool>> call(AddToWhatsAppParams params) async {
    return await repository.addToWhatsApp(params.pack);
  }
}

class AddToWhatsAppParams {
  final StickerPackEntity pack;
  
  AddToWhatsAppParams({required this.pack});
}
