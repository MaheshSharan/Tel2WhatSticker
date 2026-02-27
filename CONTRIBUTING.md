# Contributing to Tel2What

Thank you for your interest in contributing to Tel2What! This document provides guidelines and information for contributors.

## Getting Started

Before you begin, please:
1. Read our [Full Documentation](.qoder/repowiki/en/content/) to understand the project architecture
2. Check existing [Issues](https://github.com/MaheshSharan/Tel2WhatSticker/issues) and [Pull Requests](https://github.com/MaheshSharan/Tel2WhatSticker/pulls)
3. Review the [Code of Conduct](#code-of-conduct)

## Development Setup

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34
- Android NDK (for native WebP encoding)
- Git

### Setup Instructions

1. Fork and clone the repository:
```bash
git clone https://github.com/YOUR_USERNAME/Tel2WhatSticker.git
cd Tel2WhatSticker
```

2. Open in Android Studio and sync Gradle

3. Build the project:
```bash
./gradlew assembleDebug
```

4. Run on device/emulator:
```bash
./gradlew installDebug
```

## Project Architecture

Tel2What follows a clean architecture pattern with clear separation of concerns:

- **UI Layer**: Fragments + ViewModels (MVVM pattern)
- **Business Logic**: Conversion engine with decoders and encoders
- **Data Layer**: Repository pattern with Room database
- **Native Layer**: JNI bridge to libwebp for encoding

For detailed architecture documentation, see:
- [Architecture Overview](.qoder/repowiki/en/content/Architecture%20Overview/)
- [Technical Deep Dive](.qoder/repowiki/en/content/Technical%20Deep%20Dive/)

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported in [Issues](https://github.com/MaheshSharan/Tel2WhatSticker/issues)
2. If not, create a new issue with:
   - Clear title and description
   - Steps to reproduce
   - Expected vs actual behavior
   - Device info (Android version, device model)
   - Logs if applicable

### Suggesting Features

1. Check existing feature requests in [Issues](https://github.com/MaheshSharan/Tel2WhatSticker/issues)
2. Create a new issue with:
   - Clear description of the feature
   - Use case and benefits
   - Possible implementation approach (optional)

### Submitting Pull Requests

1. Create a new branch from `main`:
```bash
git checkout -b feature/your-feature-name
```

2. Make your changes following our [coding standards](#coding-standards)

3. Test your changes thoroughly:
   - Run existing tests
   - Add new tests for new functionality
   - Test on real devices

4. Commit your changes:
```bash
git commit -m "Add feature: brief description"
```

5. Push to your fork:
```bash
git push origin feature/your-feature-name
```

6. Create a Pull Request with:
   - Clear title and description
   - Reference to related issues
   - Screenshots/videos for UI changes
   - Test results

## Coding Standards

### Kotlin Style Guide

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add comments for complex logic
- Keep functions small and focused

### Code Organization

- Place UI code in `ui/` package
- Place business logic in `engine/` package
- Place data layer code in `data/` package
- Place utilities in `utils/` package

### Commit Messages

Use clear, descriptive commit messages:
- Good: "Fix memory leak in WebM decoder"
- Good: "Add support for custom sticker pack names"
- Bad: "Fix bug"
- Bad: "Update code"

### Testing

- Write unit tests for business logic
- Test on multiple Android versions (API 30+)
- Test with different sticker formats (TGS, WebM, static)
- Verify memory usage and performance

## Privacy Guidelines

Tel2What is privacy-focused. When contributing:

- **No Data Collection**: Don't add analytics or tracking
- **No Network Calls**: Except for Telegram Bot API during import
- **No Permissions**: Don't request unnecessary permissions
- **Local Processing**: All conversion must happen on-device
- **Transparency**: Document any data handling clearly

## Native Code (C++)

If contributing to native WebP encoding:

1. Follow C++ best practices
2. Ensure proper memory management (no leaks)
3. Add error handling and logging
4. Test on both arm64-v8a and armeabi-v7a architectures
5. Document JNI interfaces clearly

See [Native WebP Encoding](.qoder/repowiki/en/content/Technical%20Deep%20Dive/) documentation for details.

## Documentation

When adding features:

1. Update relevant documentation in `.qoder/repowiki/`
2. Add code comments for complex logic
3. Update README.md if needed
4. Include usage examples

## Code Review Process

1. All PRs require review before merging
2. Address review comments promptly
3. Keep PRs focused and reasonably sized
4. Be open to feedback and suggestions

## Code of Conduct

### Our Standards

- Be respectful and inclusive
- Welcome newcomers and help them learn
- Focus on constructive feedback
- Respect differing viewpoints
- Accept responsibility for mistakes

### Unacceptable Behavior

- Harassment or discriminatory language
- Personal attacks or trolling
- Publishing others' private information
- Any conduct that would be inappropriate in a professional setting

## Questions?

- Check the [Full Documentation](.qoder/repowiki/en/content/)
- Open a [Discussion](https://github.com/MaheshSharan/Tel2WhatSticker/discussions)
- Create an [Issue](https://github.com/MaheshSharan/Tel2WhatSticker/issues)

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

Thank you for contributing to Tel2What! 🎉
