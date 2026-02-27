# Changelog

All notable changes to Tel2What will be documented in this file.

## [1.1.2] - 2026-02-27

### Improvements
- **Faster WhatsApp Integration**: Improved ContentProvider performance to prevent app freezing when exporting sticker packs to WhatsApp
- **Better Export Experience**: Pack name and author fields now show validation errors if left empty, preventing export failures
- **Home Screen Error Handling**: Added user feedback when sticker packs fail to load from the database

### Fixed
- Fixed potential ANR (App Not Responding) issue when WhatsApp queries sticker pack data
- Fixed memory management in video sticker processing pipeline

### Under the Hood
- Removed ~350 lines of duplicate and unused code for better maintainability
- Consolidated image processing functions to reduce code complexity
- Added synchronous database queries optimized for ContentProvider usage

## [1.1.0] - 2026-02-26

### Added
- **Animated Sticker Support**: Full support for TGS (Lottie) and WebM video stickers
- **Native WebP Encoder**: Hardware-accelerated encoding with ~2-3s conversion time per sticker
- **Stop Button**: Cancel ongoing conversions at any time
- **Onboarding Flow**: 3-screen introduction for first-time users
- **Settings Screen**: About, developer info, and legal documents
- **Manual Upload**: Upload your own images and GIFs to create custom packs

### Changed
- **UI Redesign**: WhatsApp-inspired grey color scheme for better aesthetics
- **Performance**: Optimized conversion pipeline with adaptive quality and FPS
- **Navigation**: Improved back button handling across all screens

### Fixed
- Memory leaks in bitmap processing
- Conversion progress tracking accuracy
- Export screen navigation issues

### Technical
- Native C++ WebP encoder with JNI bridge
- Custom WebM decoder using MediaCodec
- YUV color space conversion for video frames
- Smart compression to meet 500KB WhatsApp limit

## [1.0.0] - 2025-12-15

### Added
- Initial release
- Import Telegram sticker packs via link
- Convert static stickers to WhatsApp format
- Batch processing up to 30 stickers
- Export to WhatsApp
- Local database for pack management
