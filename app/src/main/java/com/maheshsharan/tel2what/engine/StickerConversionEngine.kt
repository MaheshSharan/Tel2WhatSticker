package com.maheshsharan.tel2what.engine

import android.content.Context
import android.util.Log
import com.maheshsharan.tel2what.engine.decoder.FrameExtractor
import com.maheshsharan.tel2what.engine.decoder.TgsDecoder
import com.maheshsharan.tel2what.engine.encoder.AnimatedWebpEncoder
import com.maheshsharan.tel2what.engine.frame.FrameData
import com.maheshsharan.tel2what.engine.frame.FrameNormalizer
import com.maheshsharan.tel2what.engine.frame.FrameTimingAdjuster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File

class StickerConversionEngine(private val context: Context) {

    private val staticConverter = StaticStickerConverter()
    
    private val config = ConversionConfig()

    // Concurrency controls:
    // Static is cheap, allow up to 4 parallel processing threads.
    // Animated (FFmpeg + CPU rendering) is massively expensive. Lock to 1 to prevent generic mid-tier thermal throttling or OOM.
    private val staticSemaphore = Semaphore(4)
    private val animatedSemaphore = Semaphore(1)

    /**
     * The master orchestrator for media pipelines. Automatically routes Telegram sticker forms 
     * out to the correct processing architecture safely scaling threads under memory pressure.
     */
    suspend fun convertSticker(
        inputFile: File,
        outputFile: File,
        isAnimatedPack: Boolean
    ): StickerConversionResult {
        
        Log.i("Tel2What:Engine", "=== CONVERSION START ===")
        Log.i("Tel2What:Engine", "Input: ${inputFile.name} (${inputFile.length()} bytes)")
        Log.i("Tel2What:Engine", "Input exists: ${inputFile.exists()}")
        Log.i("Tel2What:Engine", "Output: ${outputFile.name}")
        Log.i("Tel2What:Engine", "isAnimatedPack: $isAnimatedPack")
        
        if (!inputFile.exists()) {
            Log.e("Tel2What:Engine", "ERROR: Input file does not exist!")
            return StickerConversionResult.Failed("Input file does not exist: ${inputFile.name}")
        }
        
        if (inputFile.length() == 0L) {
            Log.e("Tel2What:Engine", "ERROR: Input file is empty (0 bytes)!")
            return StickerConversionResult.Failed("Input file is empty: ${inputFile.name}")
        }
        
        val fileName = inputFile.name.lowercase()
        val isTgs = fileName.endsWith(".tgs")
        val isWebm = fileName.endsWith(".webm")
        val isAnimatedFile = isTgs || isWebm

        Log.i("Tel2What:Engine", "File analysis: fileName='$fileName'")
        Log.i("Tel2What:Engine", "File type detection: isTgs=$isTgs, isWebm=$isWebm, isAnimatedFile=$isAnimatedFile")
        
        // 2. Routing and Thread Fencing
        val pipeline = when {
            isAnimatedFile -> "ANIMATED (${if (isTgs) "TGS" else "WEBM"})"
            isAnimatedPack -> "STATIC_AS_ANIMATED"
            else -> "STATIC"
        }
        Log.i("Tel2What:Engine", "Selected pipeline: $pipeline")
        
        return if (isAnimatedFile) {
            Log.i("Tel2What:Engine", "Entering animated pipeline (isTgs=$isTgs)")
            animatedSemaphore.withPermit {
                processAnimated(inputFile, outputFile, isTgs)
            }
        } else if (isAnimatedPack) {
            Log.i("Tel2What:Engine", "Entering static-as-animated pipeline")
            // Mixed Pack Case: Turn static image into a 1-frame animation
            animatedSemaphore.withPermit {
                processStaticAsAnimated(inputFile, outputFile)
            }
        } else {
            Log.i("Tel2What:Engine", "Entering static pipeline")
            staticSemaphore.withPermit {
                staticConverter.convert(inputFile, outputFile, config)
            }
        }
    }

    private suspend fun processStaticAsAnimated(inputFile: File, outputFile: File): StickerConversionResult = withContext(Dispatchers.IO) {
        try {
            Log.i("Tel2What:Engine", "StaticAsAnimated: Decoding bitmap from ${inputFile.name}")
            val bitmap = android.graphics.BitmapFactory.decodeFile(inputFile.absolutePath)
            if (bitmap == null) {
                Log.e("Tel2What:Engine", "StaticAsAnimated: Failed to decode bitmap")
                return@withContext StickerConversionResult.Failed("Could not decode static file for animation wrapping.")
            }
            
            Log.i("Tel2What:Engine", "StaticAsAnimated: Bitmap decoded ${bitmap.width}x${bitmap.height}")
            
            // Normalize to 512x512
            val normalized = FrameNormalizer.normalizeToSubCanvas(bitmap, config.targetWidth, config.targetHeight)
            bitmap.recycle()
            Log.i("Tel2What:Engine", "StaticAsAnimated: Normalized to ${normalized.width}x${normalized.height}")

            val frames = listOf(FrameData(normalized, 1000)) // 1 frame, 1000ms duration
            Log.i("Tel2What:Engine", "StaticAsAnimated: Encoding 1 frame to animated WebP")
            
            val result = AnimatedWebpEncoder().encode(
                frames.map { it.bitmap }.toTypedArray(),
                frames.map { it.durationMs }.toIntArray(),
                outputFile,
                90
            )

            normalized.recycle()

            if (result) {
                Log.i("Tel2What:Engine", "StaticAsAnimated: Encoding successful, validating output")
                WhatsAppStickerValidator.validateOutput(outputFile, true, config)
            } else {
                Log.e("Tel2What:Engine", "StaticAsAnimated: JNI encoding failed")
                StickerConversionResult.Failed("JNI failed to wrap static image as animation.")
            }
        } catch (e: Exception) {
            Log.e("Tel2What:Engine", "StaticAsAnimated: Exception occurred", e)
            StickerConversionResult.Failed("Failed to wrap static as animated", e)
        }
    }

    private suspend fun processAnimated(inputFile: File, outputFile: File, isTgs: Boolean): StickerConversionResult = withContext(Dispatchers.IO) {
        var frames = emptyList<FrameData>()
        val startTimeMs = System.currentTimeMillis()
        try {
            Log.i("Tel2What:Engine", "ProcessAnimated: Starting ${if (isTgs) "TGS" else "WEBM"} pipeline")
            
            // STEP 1: Decoder Format Routing
            if (isTgs) {
                Log.i("Tel2What:Engine", "ProcessAnimated: Decoding TGS file")
                val composition = TgsDecoder.decode(inputFile, context.cacheDir)
                if (composition == null) {
                    Log.e("Tel2What:Engine", "ProcessAnimated: TGS decoder returned null composition")
                    return@withContext StickerConversionResult.Failed("TGS LottieComposition failed to decode.")
                }
                
                Log.i("Tel2What:Engine", "ProcessAnimated: TGS composition decoded, duration=${composition.duration}ms")
                Log.i("Tel2What:Engine", "ProcessAnimated: Extracting frames at ${config.targetFps}fps")
                
                frames = FrameExtractor.extractFromLottie(
                    composition,
                    config.targetWidth,
                    config.targetHeight,
                    config.targetFps,
                    config.maxDurationMs
                )
                
                Log.i("Tel2What:Engine", "ProcessAnimated: TGS extracted ${frames.size} frames")
            } else {
                Log.i("Tel2What:Engine", "ProcessAnimated: Decoding WEBM file")
                val rawFrames = com.maheshsharan.tel2what.engine.decoder.WebmDecoderSimple.decode(inputFile, config.targetFps, config.maxDurationMs)
                Log.i("Tel2What:Engine", "ProcessAnimated: WEBM decoder returned ${rawFrames.size} raw frames")
                
                if (rawFrames.isEmpty()) {
                    Log.e("Tel2What:Engine", "ProcessAnimated: WEBM yielded 0 frames")
                    return@withContext StickerConversionResult.Failed("WebM yielded 0 frames.")
                }
                
                Log.i("Tel2What:Engine", "ProcessAnimated: Normalizing WEBM frames to 512x512")
                // Normalizing arbitrary WebM outputs to WhatsApp 512x512 bounded padded surfaces
                frames = rawFrames.map { frame ->
                    FrameData(
                        bitmap = FrameNormalizer.normalizeToSubCanvas(frame.bitmap, config.targetWidth, config.targetHeight),
                        durationMs = frame.durationMs
                    )
                }
                Log.i("Tel2What:Engine", "ProcessAnimated: WEBM normalized ${frames.size} frames")
            }

            if (frames.isEmpty()) {
                Log.e("Tel2What:Engine", "ProcessAnimated: No frames extracted!")
                return@withContext StickerConversionResult.Failed("Failed to extract structural frames natively.")
            }

            Log.i("Tel2What:Engine", "ProcessAnimated: Total frames extracted: ${frames.size}")
            Log.i("Tel2What:Engine", "ProcessAnimated: Total duration: ${frames.sumOf { it.durationMs }}ms")

            // STEP 2: The Multi-Dimensional Compression Loop
            val encoder = AnimatedWebpEncoder()
            var currentFps = config.targetFps
            var success = false

            Log.i("Tel2What:Engine", "ProcessAnimated: Starting compression loop")
            
            pipeline@ while (currentFps >= 5 && !success) { // Floor at 5 FPS before failing out entirely
                var currentQuality = 25 // Start at 25 for maximum speed, lower quality acceptable

                Log.i("Tel2What:Engine", "ProcessAnimated: Trying FPS=$currentFps")
                
                // Floor Timing/FPS adjustments across surviving frames natively without re-rendering Lottie
                val decimatedFrames = FrameTimingAdjuster.decimateFps(frames, config.targetFps, currentFps)
                
                // Prepare JNI array shims
                val bitmaps = decimatedFrames.map { it.bitmap }.toTypedArray()
                val durations = decimatedFrames.map { it.durationMs }.toIntArray()

                Log.i("Tel2What:Metrics", "Native Run -> Extracted ${bitmaps.size} frames @ ${currentFps}fps | Total Duration sum: ${durations.sum()}ms")

                while (currentQuality >= 25) { // Do not drop below 25% lossy for acceptable quality.
                    Log.i("Tel2What:Engine", "ProcessAnimated: Encoding with quality=$currentQuality")
                    val encodedOk = encoder.encode(bitmaps, durations, outputFile, currentQuality)
                    
                    if (encodedOk) {
                        val size = outputFile.length()
                        Log.i("Tel2What:Engine", "Native Mux Complete. Size=${size/1024}KB, Q=$currentQuality, FPS=$currentFps")
                        if (size in 1..config.maxAnimatedSizeBytes) {
                            Log.i("Tel2What:Engine", "ProcessAnimated: SUCCESS! File size within limits")
                            success = true
                            break@pipeline
                        } else {
                            Log.w("Tel2What:Engine", "ProcessAnimated: File too large (${size/1024}KB > ${config.maxAnimatedSizeBytes/1024}KB), reducing quality")
                        }
                    } else {
                        Log.e("Tel2What:Engine", "Native Libwebp Enc failed at Q=$currentQuality")
                    }
                    
                    // Decimate WebP -q parameter natively
                    currentQuality -= 10
                }

                // If quality is completely depleted and it's still >500KB, drop structural FPS (timing)
                if (!success) {
                    Log.w("Tel2What:Engine", "ProcessAnimated: Quality exhausted, reducing FPS from $currentFps to ${currentFps - 5}")
                    currentFps -= 5
                }
            }
            val endTimeMs = System.currentTimeMillis()

            // Cleanup heavy memory allocation immediately once JNI execution breaks
            for (f in frames) {
                if (!f.bitmap.isRecycled) f.bitmap.recycle()
            }

            if (!success) {
                Log.e("Tel2What:Engine", "ProcessAnimated: FAILED - Could not compress under 500KB")
                return@withContext StickerConversionResult.Failed("Could not compress animation under 500KB constraint via JNI loop.")
            }
            
            Log.i("Tel2What:Engine", "ProcessAnimated: Compression successful, validating output")

            // TELEMETRY OUTPUT:
            Log.i("Tel2What:Metrics", """
                |AnimatedMetrics:
                |- initialFrameCount: ${frames.size}
                |- finalFrameCount: ${(outputFile.length() > 0)} // Inferred
                |- initialDuration: ${frames.sumOf { it.durationMs }}ms
                |- finalDuration: ${frames.sumOf { it.durationMs }}ms // Decimation preserves global time
                |- finalQualityUsed: $currentFps
                |- finalFpsUsed: $currentFps
                |- finalFileSize: ${outputFile.length() / 1024} KB
                |- encodeTimeMs: ${endTimeMs - startTimeMs} ms
            """.trimMargin())

            // Route through standardized WhatsApp constraint boundaries
            return@withContext WhatsAppStickerValidator.validateOutput(outputFile, true, config)

        } catch (e: Exception) {
            Log.e("Tel2What:Engine", "ProcessAnimated: Catastrophic pipeline failure - ${e.message}", e)
            for (f in frames) {
                if (!f.bitmap.isRecycled) f.bitmap.recycle()
            }
            return@withContext StickerConversionResult.Failed("Catastrophic pipeline failure: ${e.message}", e)
        }
    }
}
