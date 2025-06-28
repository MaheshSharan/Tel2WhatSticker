import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'core/di/injection.dart';
import 'core/router/app_router.dart';
import 'core/theme/app_theme.dart';
import 'features/sticker_converter/presentation/bloc/sticker_converter_bloc.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Initialize dependency injection
  await init();
  
  runApp(const TelegramToWhatsAppStickersApp());
}

class TelegramToWhatsAppStickersApp extends StatelessWidget {
  const TelegramToWhatsAppStickersApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiBlocProvider(
      providers: [
        BlocProvider(
          create: (context) => sl<StickerConverterBloc>(),
        ),
      ],
      child: MaterialApp.router(
        title: 'Telegram to WhatsApp Stickers',
        theme: AppTheme.lightTheme,
        darkTheme: AppTheme.darkTheme,
        routerConfig: AppRouter.router,
        localizationsDelegates: const [
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
          GlobalCupertinoLocalizations.delegate,
        ],
        supportedLocales: const [
          Locale('en', 'US'),
        ],
        debugShowCheckedModeBanner: false,
      ),
    );
  }
}
