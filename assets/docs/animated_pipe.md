# Animated Sticker Conversion System - Technical Documentation

## Executive Summary

This document outlines the architecture and implementation of the animated sticker conversion pipeline for Tel2What, enabling seamless conversion of Telegram animated stickers (TGS and WebM formats) to WhatsApp-compatible animated WebP format.

## System Architecture

### Overview

The conversion system processes two distinct animated sticker formats from Telegram and outputs WhatsApp-compliant animated WebP files with strict size and dimension constraints.

### Input Formats

1. **TGS (Telegram Sticker)**: JSON-based Lottie animations
2. **WebM**: VP9-encoded video files with transparency

### Output Requirements

- **Format**: Animated WebP
- **Dimensions**: 512x512 pixels (with aspect-ratio-preserving padding)
- **Maximum File Size**: 500KB
- **Frame Rate**: 10 FPS (optimized for performance)
- **Duration**: Maximum 10 seconds
- **Loop**: Infinite loop for WhatsApp compatibility

## Technical Implementation

### 1. Decoding Pipeline

#### TGS Decoder (`TgsDecoder.kt`)
- Utilizes Airbnb's Lottie library for JSON animation parsing
- Renders vector animations to bitmap frames
- Extracts frames at target FPS using composition duration
- Handles resolution normalization to 512x512

#### WebM Decoder (`WebmDecoderSimple.kt`)
The WebM decoder represents the most complex component of the system:

**Architecture**:
- Uses Android's MediaCodec API for hardware-accelerated VP9 decoding
- Decodes directly to ByteBuffer (YUV color space) instead of Surface rendering
- Implements manual YUV-to-RGB color space conversion

**Color Space Conversion**:
- Input: I420 format (YUV planar) from MediaCodec
- Intermediate: NV21 format conversion
- Output: RGB via Android's hardware-accelerated `YuvImage.compressToJpeg()`
- Performance: ~400ms for 30 frames

**Key Technical Decisions**:
- Avoided Surface/ImageReader approach due to format mismatch issues
- ByteBuffer approach provides direct pixel access without format conversion overhead
- Hardware-accelerated JPEG compression used as intermediate step for efficiency

### 2. Frame Processing

#### Frame Normalization (`FrameNormalizer.kt`)
- Maintains aspect ratio while fitting content within 512x512 canvas
- Adds transparent padding to achieve exact 512x512 dimensions
- Ensures WhatsApp dimension compliance

#### Frame Timing Adjustment (`FrameTimingAdjuster.kt`)
- Decimates frame rate from source to target FPS
- Preserves total animation duration
- Optimizes frame count for file size reduction

### 3. Encoding Pipeline

#### Native WebP Encoder (`webp_native_bridge.cpp`)

**JNI Bridge Architecture**:
- C++ implementation using Google's libwebp library
- Direct memory access to Android bitmap buffers
- Zero-copy frame transfer from Java to native code

**Encoding Configuration**:
```cpp
WebPConfig config;
config.quality = 25;           // Lossy compression (25%)
config.lossless = 0;           // Lossy mode enabled
config.method = 1;             // Fast encoding (0=fastest, 6=slowest)
config.anim_params.loop_count = 0;  // Infinite loop
```

**Performance Optimization**:
- Method 1 encoding: ~2-3 seconds per sticker
- Quality 25: Optimal balance between file size and visual quality
- Adaptive compression loop with fallback strategies

### 4. Adaptive Compression Strategy

The system implements a multi-dimensional compression algorithm:

```
Initial Parameters: quality=25, fps=10
│
├─ Encode with current settings
│
├─ Check file size
│   │
│   ├─ Size ≤ 500KB → Success
│   │
│   └─ Size > 500KB
│       │
│       ├─ Reduce quality by 10 (minimum: 25)
│       │
│       └─ If quality exhausted:
│           └─ Reduce FPS by 5 (minimum: 5)
│
└─ Repeat until success or failure
```

### 5. Concurrency Management

**Thread Pool Configuration**:
- Static stickers: Semaphore(4) - 4 parallel conversions
- Animated stickers: Semaphore(1) - Single-threaded processing
  - Rationale: Animated conversion is CPU-intensive and memory-heavy
  - Prevents thermal throttling and OOM crashes

**Memory Management**:
- Explicit bitmap recycling after encoding
- Immediate cleanup of intermediate frames
- Coroutine-based cancellation support

### 6. Conversion State Machine

```
DOWNLOADING → CONVERTING → READY
                ↓
              FAILED
                ↓
              STOPPED (user-initiated)
```

**State Transitions**:
- `DOWNLOADING`: Fetching file from Telegram servers
- `CONVERTING`: Active processing through decode/encode pipeline
- `READY`: Successfully converted and validated
- `FAILED`: Conversion error or validation failure
- `STOPPED`: User-cancelled mid-conversion

## Performance Metrics

### Benchmarks (Per Sticker)

| Operation | Duration | Notes |
|-----------|----------|-------|
| WebM Frame Extraction | ~400ms | 30 frames @ 10fps |
| TGS Frame Rendering | ~600ms | Vector to raster conversion |
| WebP Encoding | ~2-3s | Native libwebp, quality=25 |
| Total Conversion | ~3-4s | End-to-end pipeline |

### Output Characteristics

| Metric | Value | Constraint |
|--------|-------|------------|
| File Size | 80-150KB | < 500KB required |
| Quality | 25% lossy | Acceptable for stickers |
| Frame Rate | 10 FPS | Optimized for speed |
| Dimensions | 512x512 | WhatsApp standard |

## User Experience Features

### Progress Tracking
- Real-time conversion speed (stickers/second)
- ETA calculation based on current batch
- Overall progress across all stickers
- Per-sticker status visualization

### Conversion Control
- Stop button to cancel in-progress conversions
- Graceful state handling for stopped stickers
- Minimum 4 stickers required to proceed
- Batch processing with incremental downloads

### Error Handling
- Comprehensive logging throughout pipeline
- Specific error messages for each failure point
- Failed sticker identification in UI
- Retry capability via batch re-download

## Technical Challenges Overcome

### 1. MediaCodec Format Mismatch
**Problem**: MediaCodec outputs YUV format (0x7f000010) when rendering to Surface, but ImageReader expects RGBA (0x1).

**Solution**: Switched to ByteBuffer-based decoding with manual YUV-to-RGB conversion, eliminating format mismatch entirely.

### 2. Handler/Looper Threading
**Problem**: ImageReader requires a Handler with Looper on background threads.

**Solution**: Abandoned Surface/ImageReader approach in favor of direct ByteBuffer access.

### 3. Encoding Performance
**Problem**: Initial implementation (method=6, quality=75) took 107 seconds per sticker.

**Solution**: Iterative optimization to method=1, quality=25, achieving 2-3 second encoding time (35x speedup).

### 4. Memory Management
**Problem**: Processing 30+ animated stickers simultaneously caused OOM crashes.

**Solution**: Implemented Semaphore(1) for animated conversions and explicit bitmap recycling.

## Dependencies

### Native Libraries
- **libwebp**: Google's WebP encoding library (via CMake)
- **Android NDK**: Native development toolkit for JNI bridge

### Android Libraries
- **MediaCodec**: Hardware-accelerated video decoding
- **Lottie**: JSON animation rendering
- **Kotlin Coroutines**: Asynchronous processing

### Build Configuration
- **CMake**: Native library compilation
- **JNI**: Java-to-C++ bridge for WebP encoding
- **Gradle**: Build system integration

## Future Optimization Opportunities

1. **GPU Acceleration**: Leverage RenderScript or Vulkan for YUV conversion
2. **Parallel Frame Processing**: Process frames in parallel during extraction
3. **Adaptive Quality**: Dynamic quality adjustment based on content complexity
4. **Caching**: Cache decoded frames for retry scenarios
5. **Streaming Encoding**: Encode frames as they're decoded (pipeline parallelism)

## Validation & Quality Assurance

### WhatsApp Compliance Checks
- Dimension validation (512x512)
- File size validation (< 500KB)
- Format validation (animated WebP)
- Loop count verification (infinite)
- Frame duration validation (≥ 8ms per frame)

### Testing Coverage
- TGS sticker conversion
- WebM sticker conversion
- Mixed pack handling (static + animated)
- Large batch processing (50+ stickers)
- Stop/resume functionality
- Memory leak prevention

## Conclusion

The animated sticker conversion system successfully bridges the gap between Telegram's diverse sticker formats and WhatsApp's strict requirements. Through careful optimization of encoding parameters, efficient color space conversion, and robust concurrency management, the system achieves production-grade performance while maintaining acceptable visual quality.

The implementation demonstrates advanced Android multimedia programming, native code integration, and real-time processing optimization—delivering a seamless user experience for animated sticker conversion.
