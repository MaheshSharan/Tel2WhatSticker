import 'package:dartz/dartz.dart';
import 'package:injectable/injectable.dart';
import '../repositories/sticker_converter_repository.dart';
import '../../../../core/error/failures.dart';
import '../../../../core/usecases/usecase.dart';

@injectable
class CheckWhatsAppUseCase implements UseCase<bool, NoParams> {
  final StickerConverterRepository repository;
  
  CheckWhatsAppUseCase(this.repository);
  
  @override
  Future<Either<Failure, bool>> call(NoParams params) async {
    return await repository.isWhatsAppInstalled();
  }
}
