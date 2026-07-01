# Changelog

All notable changes to Tel2What - Telegram to WhatsApp Sticker Converter will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.3] - 2026-07-01

### Added
- Comprehensive unit tests for core conversion components
- Accessibility support with contentDescription for all sticker images
- KDoc documentation for public APIs
- ProGuard rules for release builds
- Robolectric test framework for Android unit testing
- Detekt static analysis configuration
- GitHub Actions CI pipeline (lint → test → build APK)

### Changed
- Migrated from KAPT to KSP for 2× faster build times
- Updated AndroidX dependencies (core-ktx 1.13.1, material 1.12.0)
- Improved code documentation with detailed KDoc comments
- Replaced magic numbers with named constants
- Bot token now loaded via BuildConfig from local.properties

### Removed
- Unused Jsoup dependency
- Hardcoded Telegram bot token from source code
- Legacy English documentation (79 files, 21k lines)

### Fixed
- Security: Removed exposed bot token from source code
- Build: Added ProGuard rules to prevent JNI/Room obfuscation

## [Unreleased]

## [1.1.2] - 2024-12-XX

### Added
- Performance improvements for sticker conversion
- Enhanced build configuration for proper APK signing

### Changed
- Updated documentation structure and organization
- Simplified README with links to comprehensive documentation

### Fixed
- Splash screen version display now uses BuildConfig dynamically
- Various bug fixes and stability improvements

## [1.1.1] - 2024-XX-XX

### Fixed
- Minor bug fixes and performance optimizations

## [1.1.0] - 2024-XX-XX

### Added
- Initial public release
- Telegram sticker pack import functionality
- Support for static, TGS (Lottie), and WEBM animated stickers
- Native WebP encoding via JNI for optimal performance
- Manual sticker upload feature
- WhatsApp content provider for seamless export

### Technical Highlights
- MVVM architecture with Repository pattern
- Room database for local persistence
- Hardware-accelerated MediaCodec for WEBM decoding
- Custom YUV to RGB color space conversion
- Adaptive compression loop to meet WhatsApp's 500KB limit

[Unreleased]: https://github.com/MaheshSharan/Tel2WhatSticker/compare/v1.1.2...HEAD
[1.1.2]: https://github.com/MaheshSharan/Tel2WhatSticker/releases/tag/v1.1.2
[1.1.1]: https://github.com/MaheshSharan/Tel2WhatSticker/releases/tag/v1.1.1
[1.1.0]: https://github.com/MaheshSharan/Tel2WhatSticker/releases/tag/v1.1.0
