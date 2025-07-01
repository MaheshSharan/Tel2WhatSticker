import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../features/splash/presentation/pages/splash_page.dart';
import '../../features/sticker_converter/presentation/pages/home_page.dart';
import '../../features/sticker_converter/presentation/pages/upload_page.dart';
import '../../features/sticker_converter/presentation/pages/preview_page.dart';
import '../../features/sticker_converter/presentation/pages/processing_page.dart';
import '../../features/sticker_converter/presentation/pages/success_page.dart';

class AppRouter {
  static const String splash = '/';
  static const String home = '/home';
  static const String upload = '/upload';
  static const String preview = '/preview';
  static const String processing = '/processing';
  static const String success = '/success';
  
  static final GoRouter router = GoRouter(
    initialLocation: splash,
    routes: [
      GoRoute(
        path: splash,
        name: 'splash',
        builder: (context, state) => const SplashPage(),
      ),
      GoRoute(
        path: home,
        name: 'home',
        builder: (context, state) => const HomePage(),
      ),
      GoRoute(
        path: upload,
        name: 'upload',
        builder: (context, state) {
          final inputType = state.uri.queryParameters['type'] ?? 'images';
          return UploadPage(inputType: inputType);
        },
      ),
      GoRoute(
        path: preview,
        name: 'preview',
        builder: (context, state) {
          final packData = state.extra as Map<String, dynamic>?;
          return PreviewPage(packData: packData);
        },
      ),
      GoRoute(
        path: processing,
        name: 'processing',
        builder: (context, state) {
          final processData = state.extra as Map<String, dynamic>?;
          return ProcessingPage(processData: processData);
        },
      ),
      GoRoute(
        path: success,
        name: 'success',
        builder: (context, state) {
          final resultData = state.extra as Map<String, dynamic>?;
          return SuccessPage(resultData: resultData);
        },
      ),
    ],
    errorBuilder: (context, state) => Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(
              Icons.error_outline,
              size: 64,
              color: Colors.red,
            ),
            const SizedBox(height: 16),
            Text(
              'Page not found',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 8),
            Text(
              state.error.toString(),
              style: Theme.of(context).textTheme.bodyMedium,
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: () => context.go(home),
              child: const Text('Go Home'),
            ),
          ],
        ),
      ),
    ),
  );
}
