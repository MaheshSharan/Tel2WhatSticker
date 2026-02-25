# Changelog

All notable changes to Tel2What will be documented in this file.

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
