import 'package:dartz/dartz.dart';

abstract class UseCase<Type, Params> {
  Future<Either<dynamic, Type>> call(Params params);
}

abstract class StreamUseCase<Type, Params> {
  Stream<Either<dynamic, Type>> call(Params params);
}

class NoParams {
  const NoParams();
}
