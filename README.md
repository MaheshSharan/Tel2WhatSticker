<p align="center">
  <img src="assets/logo.png" alt="Tel2What Logo" width="120"/>
</p>

<h1 align="center">Tel2What - Telegram to WhatsApp Sticker Converter</h1>

<p align="center">Convert your favorite Telegram sticker packs to WhatsApp format with ease. Fast, offline, and privacy-focused.</p>

---

## Privacy & Trust

- **No Ads**: Completely ad-free experience
- **No Data Collection**: We don't collect, store, or transmit any personal data
- **No Analytics**: No tracking or analytics services integrated
- **Fully Offline**: All processing happens locally on your device after initial download
- **Open Source**: Full source code available for review and audit
- **No Permissions Abuse**: Only requests necessary permissions for core functionality

Your privacy matters. This app is built with transparency and user trust as core principles.

## Features

- **Import from Telegram**: Paste any Telegram sticker pack link and import instantly
- **Animated Sticker Support**: Full support for TGS (Lottie) and WebM video stickers
- **Fast Conversion**: Hardware-accelerated processing with native WebP encoding
- **Fully Offline**: All processing happens on your device after initial download
- **Batch Processing**: Download and convert up to 30 stickers at a time
- **Manual Upload**: Upload your own images and GIFs to create custom packs
- **WhatsApp Ready**: Automatic validation and optimization for WhatsApp requirements

## Technical Highlights

- **Native Performance**: JNI bridge to libwebp for optimal encoding speed (~2-3s per animated sticker)
- **Advanced Video Decoding**: Custom WebM decoder using MediaCodec with YUV color space conversion
- **Smart Compression**: Adaptive quality and FPS adjustment to meet 500KB size limit
- **Memory Efficient**: Proper bitmap recycling and concurrency management to prevent OOM
- **Modern Architecture**: Single Activity + Fragments, Kotlin Coroutines, Room Database

##  Screenshots

<table>
  <tr>
    <td align="center"><b>Splash</b></td>
    <td align="center"><b>Onboarding</b></td>
    <td align="center"><b>Home</b></td>
  </tr>
  <tr>
    <td><img src="screenshots/splash.jpg" width="250"/></td>
    <td><img src="screenshots/onboarding1.jpg" width="250"/></td>
    <td><img src="screenshots/home.jpg" width="250"/></td>
  </tr>
</table>

<table>
  <tr>
    <td align="center"><b>Import</b></td>
    <td align="center"><b>Conversion</b></td>
    <td align="center"><b>Selection</b></td>
  </tr>
  <tr>
    <td><img src="screenshots/import.jpg" width="250"/></td>
    <td><img src="screenshots/conversion.jpg" width="250"/></td>
    <td><img src="screenshots/selection.jpg" width="250"/></td>
  </tr>
</table>

<table>
  <tr>
    <td align="center"><b>Tray Icon</b></td>
    <td align="center"><b>Export</b></td>
    <td align="center"><b>Manual Upload</b></td>
  </tr>
  <tr>
    <td><img src="screenshots/tray.jpg" width="250"/></td>
    <td><img src="screenshots/export.jpg" width="250"/></td>
    <td><img src="screenshots/manual.jpg" width="250"/></td>
  </tr>
</table>

## Requirements

- Android 11 (API 30) or higher
- 100MB free storage (for temporary conversion files)
- Telegram Bot Token (for importing sticker packs)

## Tech Stack

- **Language**: Kotlin
- **UI**: Material Design 3, Navigation Component
- **Database**: Room
- **Async**: Kotlin Coroutines + Flow
- **Networking**: OkHttp + Retrofit
- **Image Loading**: Glide
- **Native**: C++ with CMake, libwebp
- **Animation**: Lottie (for TGS stickers)

## Architecture

```
app/
├── cpp/                    # Native WebP encoder (JNI)
├── engine/                 # Conversion pipeline
│   ├── decoder/           # TGS & WebM decoders
│   ├── encoder/           # Animated WebP encoder
│   └── frame/             # Frame processing utilities
├── data/                  # Repository & data sources
├── ui/                    # Fragments & ViewModels
└── utils/                 # Helper utilities
```

## Building

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34
- NDK (for native WebP encoding)

### Telegram Bot Token

The app uses the Telegram Bot API to fetch sticker metadata. A demo bot token is included in the source for quick testing. For production use or if you encounter rate limits, create your own bot:

1. Message [@BotFather](https://t.me/BotFather) on Telegram
2. Create a new bot with `/newbot`
3. Replace the token in `TelegramBotApi.kt`:
   ```kotlin
   private val botToken = "YOUR_BOT_TOKEN_HERE"
   ```

**Note**: The included token is for a read-only bot with no access to private data. It's safe for testing but may hit rate limits with heavy usage.

### Build Commands

```bash
# Clone the repository
git clone https://github.com/MaheshSharan/Tel2WhatSticker.git
cd Tel2WhatSticker

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Build release APK
./gradlew assembleRelease
```

## Performance Metrics

| Operation | Duration | Notes |
|-----------|----------|-------|
| WebM Frame Extraction | ~400ms | 30 frames @ 10fps |
| TGS Frame Rendering | ~600ms | Vector to raster |
| WebP Encoding | ~2-3s | Native libwebp, quality=25 |
| Total Conversion | ~3-4s | End-to-end per sticker |

## Documentation

- [Animated Sticker Pipeline](assets/docs/animated_pipe.md) - Technical deep dive into the conversion system

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- [libwebp](https://developers.google.com/speed/webp) - Google's WebP library
- [Lottie Android](https://github.com/airbnb/lottie-android) - Airbnb's animation library
- [WhatsApp Stickers](https://github.com/WhatsApp/stickers) - WhatsApp's official sticker implementation reference
- Telegram for their excellent sticker ecosystem
