# Performance and Optimization

<cite>
**Referenced Files in This Document**
- [AnimatedWebpEncoder.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/encoder/AnimatedWebpEncoder.kt)
- [webp_native_bridge.cpp](file://app/src/main/cpp/webp_native_bridge.cpp)
- [FrameNormalizer.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/frame/FrameNormalizer.kt)
- [FrameTimingAdjuster.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/frame/FrameTimingAdjuster.kt)
- [StaticStickerConverter.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/StaticStickerConverter.kt)
- [ConversionConfig.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/ConversionConfig.kt)
- [WhatsAppStickerValidator.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/WhatsAppStickerValidator.kt)
- [WebmDecoderSimple.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/decoder/WebmDecoderSimple.kt)
- [ImageProcessor.kt](file://app/src/main/java/com/maheshsharan/tel2what/utils/ImageProcessor.kt)
- [webp_enc.c](file://app/src/main/cpp/libwebp/src/enc/webp_enc.c)
- [encode.h](file://app/src/main/cpp/libwebp/src/webp/encode.h)
- [quant_enc.c](file://app/src/main/cpp/libwebp/src/enc/quant_enc.c)
- [picture_enc.c](file://app/src/main/cpp/libwebp/src/enc/picture_enc.c)
- [sharpyuv_csp.c](file://app/src/main/cpp/libwebp/sharpyuv/sharpyuv_csp.c)
- [image_enc.c](file://app/src/main/cpp/libwebp/imageio/image_enc.c)
- [nalloc.h](file://app/src/main/cpp/libwebp/tests/fuzzer/nalloc.h)
</cite>

## Table of Contents
1. [Introduction](#introduction)
2. [Project Structure](#project-structure)
3. [Core Components](#core-components)
4. [Architecture Overview](#architecture-overview)
5. [Detailed Component Analysis](#detailed-component-analysis)
6. [Dependency Analysis](#dependency-analysis)
7. [Performance Considerations](#performance-considerations)
8. [Troubleshooting Guide](#troubleshooting-guide)
9. [Conclusion](#conclusion)

## Introduction
This document provides comprehensive performance optimization guidance for Tel2What, focusing on native WebP encoding, memory management, concurrency controls, adaptive compression, frame-rate optimization, video decoding performance, and profiling techniques. It synthesizes the repository’s Kotlin and C++ implementations to deliver actionable recommendations for meeting WhatsApp size and timing constraints while preventing OutOfMemory errors across diverse device capabilities.

## Project Structure
Tel2What’s performance-critical pipeline spans Kotlin coroutines and native C++ via JNI:
- Video ingestion and decoding: MediaCodec-based ByteBuffer extraction and YUV-to-RGB conversion.
- Frame normalization and timing: Aspect-ratio preserving scaling and frame-rate decimation.
- Static and animated WebP generation: Lossy compression with iterative quality tuning and native muxing.
- Validation: Pre-flight checks against WhatsApp constraints to avoid runtime rejections.

```mermaid
graph TB
subgraph "Kotlin Layer"
A["WebmDecoderSimple.kt"]
B["FrameNormalizer.kt"]
C["FrameTimingAdjuster.kt"]
D["StaticStickerConverter.kt"]
E["AnimatedWebpEncoder.kt"]
F["WhatsAppStickerValidator.kt"]
G["ImageProcessor.kt"]
H["ConversionConfig.kt"]
end
subgraph "JNI Bridge"
J["webp_native_bridge.cpp"]
end
subgraph "Native WebP (libwebp)"
K["webp_enc.c"]
L["encode.h"]
M["quant_enc.c"]
N["picture_enc.c"]
O["sharpyuv_csp.c"]
P["image_enc.c"]
end
A --> B --> C --> E
D --> F
E --> J
J --> K
K --> L
K --> M
K --> N
K --> O
K --> P
```

**Diagram sources**
- [WebmDecoderSimple.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/decoder/WebmDecoderSimple.kt#L1-L256)
- [FrameNormalizer.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/frame/FrameNormalizer.kt#L1-L62)
- [FrameTimingAdjuster.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/frame/FrameTimingAdjuster.kt#L1-L72)
- [StaticStickerConverter.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/StaticStickerConverter.kt#L1-L94)
- [AnimatedWebpEncoder.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/encoder/AnimatedWebpEncoder.kt#L1-L91)
- [WhatsAppStickerValidator.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/WhatsAppStickerValidator.kt#L1-L50)
- [ImageProcessor.kt](file://app/src/main/java/com/maheshsharan/tel2what/utils/ImageProcessor.kt#L1-L34)
- [webp_native_bridge.cpp](file://app/src/main/cpp/webp_native_bridge.cpp#L1-L148)
- [webp_enc.c](file://app/src/main/cpp/libwebp/src/enc/webp_enc.c#L79-L107)
- [encode.h](file://app/src/main/cpp/libwebp/src/webp/encode.h#L123-L143)
- [quant_enc.c](file://app/src/main/cpp/libwebp/src/enc/quant_enc.c#L269-L415)
- [picture_enc.c](file://app/src/main/cpp/libwebp/src/enc/picture_enc.c#L202-L247)
- [sharpyuv_csp.c](file://app/src/main/cpp/libwebp/sharpyuv/sharpyuv_csp.c#L1-L39)
- [image_enc.c](file://app/src/main/cpp/libwebp/imageio/image_enc.c#L294-L334)

**Section sources**
- [WebmDecoderSimple.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/decoder/WebmDecoderSimple.kt#L1-L256)
- [AnimatedWebpEncoder.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/encoder/AnimatedWebpEncoder.kt#L1-L91)
- [webp_native_bridge.cpp](file://app/src/main/cpp/webp_native_bridge.cpp#L1-L148)

## Core Components
- Video decoder: Extracts frames at target FPS, converts YUV_420_888 to RGB efficiently, and caps duration to meet WhatsApp limits.
- Frame normalizer: Preserves aspect ratio and fills to exact target bounds with transparent letterboxes.
- Timing adjuster: Decimates frames to reduce bitrate and file size while respecting minimum per-frame duration and total animation duration.
- Static converter: Iterative WebP compression loop to guarantee size compliance under constraints.
- Animated encoder: JNI bridge to native WebP animation muxer with lossy compression and fast method selection.
- Validator: Pre-flight size and dimension checks to fail fast and avoid downstream failures.
- Tray icon processor: Efficient WebP_LOSSY generation for small icons with quality ramp.

**Section sources**
- [WebmDecoderSimple.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/decoder/WebmDecoderSimple.kt#L1-L256)
- [FrameNormalizer.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/frame/FrameNormalizer.kt#L1-L62)
- [FrameTimingAdjuster.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/frame/FrameTimingAdjuster.kt#L1-L72)
- [StaticStickerConverter.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/StaticStickerConverter.kt#L1-L94)
- [AnimatedWebpEncoder.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/encoder/AnimatedWebpEncoder.kt#L1-L91)
- [WhatsAppStickerValidator.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/WhatsAppStickerValidator.kt#L1-L50)
- [ImageProcessor.kt](file://app/src/main/java/com/maheshsharan/tel2what/utils/ImageProcessor.kt#L1-L34)

## Architecture Overview
The pipeline integrates Kotlin coroutines with native WebP encoding through JNI. Decoding uses MediaCodec in ByteBuffer mode to minimize overhead, followed by normalization and optional frame-rate reduction. Encoding leverages libwebp’s animation encoder with tuned configuration for speed and size.

```mermaid
sequenceDiagram
participant UI as "UI"
participant DEC as "WebmDecoderSimple"
participant FN as "FrameNormalizer"
participant TA as "FrameTimingAdjuster"
participant ENC as "AnimatedWebpEncoder"
participant BR as "webp_native_bridge"
participant NW as "libwebp"
UI->>DEC : "decode(webmFile, targetFps, maxDurationMs)"
DEC-->>UI : "List<FrameData>"
UI->>FN : "normalizeToSubCanvas(frameBitmap, targetWidth, targetHeight)"
FN-->>UI : "Normalized ARGB_8888 Bitmap"
UI->>TA : "decimateFps(frames, currentFps, targetFps)"
TA-->>UI : "Reduced frames with adjusted durations"
UI->>ENC : "encode(frames, durationsMs, outputFile, quality)"
ENC->>BR : "encodeAnimatedWebpNative(bitmaps, durations, width, height, quality)"
BR->>NW : "WebPAnimEncoderAdd/Assemble"
NW-->>BR : "WebPData"
BR-->>ENC : "ByteArray"
ENC-->>UI : "true/false"
```

**Diagram sources**
- [WebmDecoderSimple.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/decoder/WebmDecoderSimple.kt#L23-L192)
- [FrameNormalizer.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/frame/FrameNormalizer.kt#L17-L60)
- [FrameTimingAdjuster.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/frame/FrameTimingAdjuster.kt#L16-L70)
- [AnimatedWebpEncoder.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/encoder/AnimatedWebpEncoder.kt#L32-L78)
- [webp_native_bridge.cpp](file://app/src/main/cpp/webp_native_bridge.cpp#L14-L147)

## Detailed Component Analysis

### Native WebP Encoding Performance
- Method and quality: Fast method selection and lossy compression are configured to balance speed and size.
- Memory growth: Dynamic buffer resizing prevents repeated reallocations and reduces fragmentation risk.
- Tool mapping: Config-to-tools mapping selects RD optimization levels and trellis based on method.

```mermaid
flowchart TD
Start(["Encode Entry"]) --> InitCfg["Initialize WebPConfig<br/>quality, method, lossless"]
InitCfg --> InitEnc["Initialize WebPAnimEncoder<br/>options with loop_count=0"]
InitEnc --> LoopFrames{"More frames?"}
LoopFrames --> |Yes| ImportRGBA["Import RGBA from AndroidBitmap"]
ImportRGBA --> AddFrame["WebPAnimEncoderAdd(frame, timestamp, config)"]
AddFrame --> NextTs["Advance timestamp"]
NextTs --> LoopFrames
LoopFrames --> |No| Finalize["WebPAnimEncoderAssemble -> WebPData"]
Finalize --> Return(["Return ByteArray"])
```

**Diagram sources**
- [webp_native_bridge.cpp](file://app/src/main/cpp/webp_native_bridge.cpp#L28-L131)
- [webp_enc.c](file://app/src/main/cpp/libwebp/src/enc/webp_enc.c#L99-L107)
- [encode.h](file://app/src/main/cpp/libwebp/src/webp/encode.h#L123-L143)

**Section sources**
- [webp_native_bridge.cpp](file://app/src/main/cpp/webp_native_bridge.cpp#L28-L131)
- [webp_enc.c](file://app/src/main/cpp/libwebp/src/enc/webp_enc.c#L99-L107)
- [encode.h](file://app/src/main/cpp/libwebp/src/webp/encode.h#L123-L143)

### Adaptive Compression and Quality Tuning
- Static WebP: Iterative compression loop lowers quality in steps until the output fits under the static size cap.
- Animated WebP: Target quality passed via JNI; combined with fast method and lossy encoding to meet animated size limits.
- Quantization and SNS: Quantization tables and segmental adjustments adapt to content complexity and user-specified strength.

```mermaid
flowchart TD
S0(["Static Input"]) --> Decode["Decode to Bitmap"]
Decode --> Scale["Scale to fit target bounds"]
Scale --> Pad["Pad to exact target with transparent background"]
Pad --> Compress["Iterative WebP compression loop"]
Compress --> SizeCheck{"Size <= maxStaticSizeBytes?"}
SizeCheck --> |Yes| Done["Success"]
SizeCheck --> |No| LowerQ["Decrease quality by fixed step"] --> Compress
```

**Diagram sources**
- [StaticStickerConverter.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/StaticStickerConverter.kt#L50-L72)
- [encode.h](file://app/src/main/cpp/libwebp/src/webp/encode.h#L123-L143)
- [quant_enc.c](file://app/src/main/cpp/libwebp/src/enc/quant_enc.c#L386-L415)

**Section sources**
- [StaticStickerConverter.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/StaticStickerConverter.kt#L50-L72)
- [quant_enc.c](file://app/src/main/cpp/libwebp/src/enc/quant_enc.c#L386-L415)

### Frame Rate Optimization and Timing Control
- Decimation: Reduces frame count by accumulating durations and enforcing minimum per-frame duration and total animation duration.
- Constraints: Enforces minimum frame duration and caps total animation length to satisfy client requirements.

```mermaid
flowchart TD
A0(["Input Frames"]) --> CalcScale["Compute scale factor = currentFps/targetFps"]
CalcScale --> Accum["Accumulate duration and skip counter"]
Accum --> Threshold{"skipCounter >= 1?"}
Threshold --> |Yes| CapMin["Cap duration to minFrameDurationMs"]
CapMin --> CapTotal["Cap total duration to maxDurationMs"]
CapTotal --> Push["Push retained frame with new duration"]
Push --> Reset["Reset counters"] --> Accum
Threshold --> |No| Absorb["Absorb duration and continue"] --> Accum
```

**Diagram sources**
- [FrameTimingAdjuster.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/frame/FrameTimingAdjuster.kt#L16-L70)

**Section sources**
- [FrameTimingAdjuster.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/frame/FrameTimingAdjuster.kt#L16-L70)
- [ConversionConfig.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/ConversionConfig.kt#L3-L13)

### Video Decoding Performance Optimizations
- ByteBuffer decoding: Avoids surface rendering overhead and reduces format mismatches.
- Codec fallback: Attempts VP9 first, falls back to VP8 if necessary.
- YUV conversion: Uses YuvImage + JPEG compression path for fast conversion to RGB, then Bitmap creation.
- Duration capping: Stops decoding beyond the configured maximum duration.

```mermaid
flowchart TD
D0(["Open WebM"]) --> Select["Select video track"]
Select --> CreateCodec["Create decoder by MIME (VP9 -> VP8 fallback)"]
CreateCodec --> Configure["Configure without surface (ByteBuffer)"]
Configure --> Loop{"Decode loop"}
Loop --> Pull["Pull input buffers"]
Pull --> Push["Push output buffers"]
Push --> Capture{"Time >= nextTargetTimeUs<br/>and <= maxDuration?"}
Capture --> |Yes| Convert["YUV_420_888 -> NV21 -> YuvImage -> JPEG -> Bitmap"]
Convert --> Store["Store FrameData"]
Store --> Loop
Capture --> |No| Loop
Loop --> EOS{"Output EOS?"}
EOS --> |No| Loop
EOS --> |Yes| Done(["Return frames"])
```

**Diagram sources**
- [WebmDecoderSimple.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/decoder/WebmDecoderSimple.kt#L89-L192)
- [WebmDecoderSimple.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/decoder/WebmDecoderSimple.kt#L205-L254)

**Section sources**
- [WebmDecoderSimple.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/decoder/WebmDecoderSimple.kt#L89-L192)
- [WebmDecoderSimple.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/decoder/WebmDecoderSimple.kt#L205-L254)

### Memory Management and Concurrency Controls
- Bitmap lifecycle: Normalize and converter routines recycle intermediate bitmaps to reduce heap pressure and fragmentation.
- Concurrency: Decoding runs on Dispatchers.IO; yields are used to keep UI responsive.
- JNI boundaries: Proper locking/unlocking of bitmap pixels and releasing local references to prevent leaks.
- Buffer growth: Dynamic memory writer resizes buffers safely to avoid repeated allocations.

```mermaid
flowchart TD
M0(["Decode Frame"]) --> Lock["Lock AndroidBitmap pixels"]
Lock --> Import["Import RGBA into WebPPicture"]
Import --> Unlock["Unlock pixels"]
Unlock --> FreePic["Free WebPPicture"]
FreePic --> Recycle["Recycle source Bitmap if needed"]
Recycle --> Next["Next frame"]
```

**Diagram sources**
- [webp_native_bridge.cpp](file://app/src/main/cpp/webp_native_bridge.cpp#L66-L111)
- [FrameNormalizer.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/frame/FrameNormalizer.kt#L52-L57)
- [StaticStickerConverter.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/StaticStickerConverter.kt#L88-L91)

**Section sources**
- [webp_native_bridge.cpp](file://app/src/main/cpp/webp_native_bridge.cpp#L66-L111)
- [FrameNormalizer.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/frame/FrameNormalizer.kt#L52-L57)
- [StaticStickerConverter.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/StaticStickerConverter.kt#L88-L91)
- [picture_enc.c](file://app/src/main/cpp/libwebp/src/enc/picture_enc.c#L202-L247)

### Validation and Size Compliance
- Pre-flight checks: Validates file existence, emptiness, and size against static/animated limits.
- Dimension check: Uses BitmapFactory with inJustDecodeBounds to quickly fetch dimensions without loading full pixel data.

```mermaid
flowchart TD
V0(["Validate Output"]) --> Exists{"File exists and not empty?"}
Exists --> |No| Fail["Return Failed"]
Exists --> |Yes| SizeCheck{"Static or Animated?"}
SizeCheck --> |Animated| CheckA["Size <= maxAnimatedSizeBytes?"]
SizeCheck --> |Static| CheckS["Size <= maxStaticSizeBytes?"]
CheckA --> |No| VFail["Return ValidationFailed"]
CheckA --> |Yes| Bounds["Decode bounds (inJustDecodeBounds)"]
CheckS --> |No| VFail
CheckS --> |Yes| Bounds
Bounds --> DimOK{"Dimensions within limits?"}
DimOK --> |No| VFail
DimOK --> |Yes| Pass["Return Success"]
```

**Diagram sources**
- [WhatsAppStickerValidator.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/WhatsAppStickerValidator.kt#L14-L50)

**Section sources**
- [WhatsAppStickerValidator.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/WhatsAppStickerValidator.kt#L14-L50)

## Dependency Analysis
Key performance dependencies:
- Kotlin coroutines drive I/O-bound tasks (decoding, compression).
- JNI bridges native WebP encoding to Kotlin.
- libwebp provides encoding tools, quantization, and memory management primitives.
- Android APIs (MediaCodec, Bitmap, YuvImage) underpin decoding and conversion.

```mermaid
graph LR
K1["WebmDecoderSimple.kt"] --> A1["MediaCodec"]
K2["StaticStickerConverter.kt"] --> A2["BitmapFactory/Compress"]
K3["AnimatedWebpEncoder.kt"] --> J1["webp_native_bridge.cpp"]
J1 --> L1["libwebp: webp_enc.c"]
L1 --> L2["libwebp: encode.h"]
L1 --> L3["libwebp: quant_enc.c"]
L1 --> L4["libwebp: picture_enc.c"]
L1 --> L5["libwebp: sharpyuv_csp.c"]
L1 --> L6["libwebp: image_enc.c"]
```

**Diagram sources**
- [WebmDecoderSimple.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/decoder/WebmDecoderSimple.kt#L89-L192)
- [StaticStickerConverter.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/StaticStickerConverter.kt#L50-L72)
- [AnimatedWebpEncoder.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/encoder/AnimatedWebpEncoder.kt#L32-L78)
- [webp_native_bridge.cpp](file://app/src/main/cpp/webp_native_bridge.cpp#L28-L131)
- [webp_enc.c](file://app/src/main/cpp/libwebp/src/enc/webp_enc.c#L99-L107)
- [encode.h](file://app/src/main/cpp/libwebp/src/webp/encode.h#L123-L143)
- [quant_enc.c](file://app/src/main/cpp/libwebp/src/enc/quant_enc.c#L386-L415)
- [picture_enc.c](file://app/src/main/cpp/libwebp/src/enc/picture_enc.c#L202-L247)
- [sharpyuv_csp.c](file://app/src/main/cpp/libwebp/sharpyuv/sharpyuv_csp.c#L1-L39)
- [image_enc.c](file://app/src/main/cpp/libwebp/imageio/image_enc.c#L294-L334)

**Section sources**
- [WebmDecoderSimple.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/decoder/WebmDecoderSimple.kt#L89-L192)
- [AnimatedWebpEncoder.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/encoder/AnimatedWebpEncoder.kt#L32-L78)
- [webp_native_bridge.cpp](file://app/src/main/cpp/webp_native_bridge.cpp#L28-L131)

## Performance Considerations
- Encoding speed vs quality: Use fast method and moderate quality for animated WebP; iterate quality for static WebP to meet size targets.
- Frame-rate decimation: Reduce FPS to decrease bitrate and file size; enforce minimum per-frame duration and total animation duration.
- Memory footprint: Normalize frames to exact target size, recycle intermediate bitmaps, and leverage dynamic buffer growth in native code.
- Hardware acceleration: Prefer MediaCodec for decoding; ensure proper fallback to VP8 when needed.
- Color space conversion: Use efficient YUV-to-RGB conversion path and avoid unnecessary format conversions.
- Concurrency: Offload I/O-bound work to Dispatchers.IO and yield periodically to maintain responsiveness.
- Validation-first: Validate size and dimensions early to avoid wasted computation.

[No sources needed since this section provides general guidance]

## Troubleshooting Guide
- JNI load failures: Verify library loading and handle UnsatisfiedLinkError gracefully.
- Empty or missing files: Validate existence and size before processing.
- Excessive memory usage: Monitor bitmap lifecycles and ensure recycling; consider reducing frame count or resolution.
- Slow decoding: Confirm codec availability and fallback; ensure ByteBuffer mode is used.
- Validation failures: Check size and dimension limits; log detected values for diagnostics.

**Section sources**
- [AnimatedWebpEncoder.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/encoder/AnimatedWebpEncoder.kt#L13-L20)
- [WhatsAppStickerValidator.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/WhatsAppStickerValidator.kt#L19-L40)
- [WebmDecoderSimple.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/decoder/WebmDecoderSimple.kt#L32-L40)

## Conclusion
Tel2What’s performance strategy combines efficient video decoding, precise frame normalization and timing, adaptive compression, and robust validation to consistently produce WhatsApp-compliant stickers. By tuning encoding parameters, controlling memory usage, leveraging hardware acceleration, and applying targeted frame-rate optimizations, the system achieves reliable performance across a wide range of devices while minimizing the risk of OutOfMemory errors.