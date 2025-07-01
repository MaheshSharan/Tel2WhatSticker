# Telegram to WhatsApp Stickers Converter
>⚠️ **WARNING: This project is in aggressive active development.**

A Flutter mobile application that converts images and Telegram sticker packs to WhatsApp sticker format with direct integration to WhatsApp.

## Features

### Input Methods
- Upload individual image files (PNG, JPG, JPEG, GIF, WebP)
- Upload ZIP archives containing multiple images
- Process Telegram sticker pack URLs

### Image Processing
- Automatic conversion to WhatsApp-compatible format
- Resizing to optimal dimensions (512x512px maximum)
- **Individual sticker compression to under 100KB** (WhatsApp requirement)
- Full support for both static and animated stickers
- Progressive download with real-time progress indicators
- WebM to WebP conversion for animated Telegram stickers
- Aggressive compression with quality reduction when needed

### WhatsApp Integration
- Direct "Add to WhatsApp" functionality
- Automatic sticker pack validation
- Seamless integration with WhatsApp application

## Requirements

### System Requirements
- Android 5.0 (API level 21) or higher
- WhatsApp installed on the device
- Minimum 100MB free storage space

### Development Requirements
- Flutter SDK 3.x or higher
- Dart SDK 3.x or higher
- Android Studio or VS Code
- Android SDK with API level 21+

## Installation

### For Users
1. Download the APK from the releases section
2. Enable installation from unknown sources in Android settings
3. Install the APK file
4. Launch the application

### For Developers
1. Clone the repository
```bash
git clone https://github.com/MaheshSharan/Tel2WhatSticker
cd telegram_to_whatsapp_stickers
```

2. Install dependencies
```bash
flutter pub get
```

3. Run code generation
```bash
dart run build_runner build --delete-conflicting-outputs
```

4. Run the application
```bash
flutter run
```

## Usage

### Basic Image Upload
1. Open the application
2. Select "Upload Images" tab
3. Choose image files or drag and drop them
4. Configure pack name and publisher
5. Tap "Create Sticker Pack"
6. Tap "Add to WhatsApp" when processing is complete

### ZIP File Processing
1. Select "Upload ZIP" tab
2. Choose a ZIP file containing images
3. The app will extract and validate images automatically
4. Configure pack details and proceed as above

### Telegram Sticker Pack Conversion
1. Select "Telegram URL" tab
2. Paste a Telegram sticker pack URL (t.me/addstickers/...)
3. Watch real-time download progress for each sticker
4. Animated stickers are automatically converted from WebM to WebP
5. Enter pack name and publisher information
6. Process and add to WhatsApp

## Supported Formats

### Input Formats
- PNG (recommended)
- JPG/JPEG
- WebP
- GIF (converted to static)
- WebM (animated - converted to WebP)
- BMP

### Output Format
- WebP (WhatsApp standard)
- Automatic compression and resizing
- Tray image generation (96x96px PNG)

## Limitations

- Maximum 30 stickers per pack (WhatsApp limitation)
- **Each individual sticker must be under 100KB** (WhatsApp requirement)
- Full animated sticker support with WebM to WebP conversion
- ZIP files larger than 50MB may cause performance issues
- Very high resolution images may require aggressive compression

## Technical Details

### Architecture
- Clean Architecture with BLoC pattern
- Repository pattern for data layer
- Dependency injection using GetIt
- State management with flutter_bloc

### Key Dependencies
- flutter_bloc: State management
- image: Image processing
- archive: ZIP file handling
- file_picker: File selection
- path_provider: File system access
- whatsapp_stickers_handler: WhatsApp integration
- ffmpeg_kit_flutter: Animated sticker conversion

## Troubleshooting

### Common Issues

**"WhatsApp not installed" error**
- Ensure WhatsApp is installed and updated
- Check that WhatsApp supports sticker packs

**Images not processing**
- Verify image formats are supported
- Check that images are not corrupted
- Ensure sufficient storage space

**ZIP extraction fails**
- Verify ZIP file is not corrupted
- Check that ZIP contains supported image formats
- Ensure ZIP file size is reasonable

**App crashes during processing**
- Close other apps to free memory
- Try processing fewer images at once
- Restart the application

**Sticker size compression issues**
- App automatically compresses images to under 100KB per sticker
- Very high resolution images may require aggressive compression
- If compression fails, try using smaller source images
- Check console output for detailed compression information

## Contributing

### Development Setup
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests: `flutter test`
5. Submit a pull request

### Code Standards
- Follow Dart/Flutter style guidelines
- Use meaningful commit messages
- Add tests for new features
- Update documentation as needed

## License

This project is licensed under the MIT License. See the LICENSE file for details.

## Support

For issues and questions:
- Create an issue on the GitHub repository
- Provide detailed information about the problem
- Include device information and error logs when applicable

## Version History

### v2.0.0 (Latest)
- **Full animated sticker support** with WebM to WebP conversion
- Progressive Telegram sticker download with real-time progress indicators
- Unified upload flow for images and Telegram stickers
- Enhanced UI/UX with dynamic progress grid
- Removed legacy code and improved architecture
- 30-sticker selection limit enforcement
- Comprehensive error handling and debug logging

### v1.0.0
- Initial release
- Basic image upload and conversion
- WhatsApp integration
- ZIP file processing
- Telegram URL support
