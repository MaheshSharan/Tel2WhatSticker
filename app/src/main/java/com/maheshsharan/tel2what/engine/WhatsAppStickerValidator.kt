package com.maheshsharan.tel2what.engine

import android.graphics.BitmapFactory
import android.util.Log
import java.io.File

/**
 * Validates converted sticker files against WhatsApp's strict requirements.
 *
 * WhatsApp enforces the following constraints for sticker packs:
 * - Exact dimensions: 512×512 pixels
 * - Static stickers: ≤100KB file size
 * - Animated stickers: ≤500KB file size
 * - Frame duration: ≥8ms per frame (for animated)
 * - Total duration: ≤10 seconds (for animated)
 *
 * This validator catches non-compliant files before export, providing detailed
 * error messages rather than silent rejection by WhatsApp.
 */
object WhatsAppStickerValidator {

    /**
     * Validates a converted WebP sticker file against WhatsApp constraints.
     *
     * @param file The output WebP file to validate
     * @param isAnimated Whether this is an animated sticker (affects size limits)
     * @param config Conversion configuration containing constraint thresholds
     * @return [StickerConversionResult.Success] if valid, [StickerConversionResult.ValidationFailed] if constraints violated
     */
    fun validateOutput(file: File, isAnimated: Boolean, config: ConversionConfig): StickerConversionResult {
        Log.i("Tel2What:Validator", "=== VALIDATION START ===")
        Log.i("Tel2What:Validator", "File: ${file.name}")
        Log.i("Tel2What:Validator", "isAnimated: $isAnimated")
        
        if (!file.exists() || file.length() == 0L) {
            Log.e("Tel2What:Validator", "VALIDATION FAILED: File does not exist or is empty")
            Log.e("Tel2What:Validator", "File exists: ${file.exists()}, size: ${file.length()}")
            return StickerConversionResult.Failed("Output file does not exist or is empty.")
        }

        val sizeBytes = file.length()
        Log.i("Tel2What:Validator", "File size: ${sizeBytes} bytes (${sizeBytes/1024}KB)")
        
        if (isAnimated) {
            Log.i("Tel2What:Validator", "Max animated size: ${config.maxAnimatedSizeBytes} bytes (${config.maxAnimatedSizeBytes/1024}KB)")
            if (sizeBytes > config.maxAnimatedSizeBytes) {
                Log.e("Tel2What:Validator", "VALIDATION FAILED: Animated sticker exceeds size limit")
                return StickerConversionResult.ValidationFailed("Animated sticker exceeds ${config.maxAnimatedSizeBytes / 1024}KB limit (Actual: ${sizeBytes / 1024}KB).")
            }
        } else {
            Log.i("Tel2What:Validator", "Max static size: ${config.maxStaticSizeBytes} bytes (${config.maxStaticSizeBytes/1024}KB)")
            if (sizeBytes > config.maxStaticSizeBytes) {
                Log.e("Tel2What:Validator", "VALIDATION FAILED: Static sticker exceeds size limit")
                return StickerConversionResult.ValidationFailed("Static sticker exceeds ${config.maxStaticSizeBytes / 1024}KB limit (Actual: ${sizeBytes / 1024}KB).")
            }
        }

        // Validate Dimensions using just the bounds (fastest)
        Log.i("Tel2What:Validator", "Validating dimensions...")
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        
        val width = options.outWidth
        val height = options.outHeight
        
        Log.i("Tel2What:Validator", "Detected dimensions: ${width}x${height}")
        Log.i("Tel2What:Validator", "Expected dimensions: ${config.targetWidth}x${config.targetHeight}")

        if (width != config.targetWidth || height != config.targetHeight) {
            Log.e("Tel2What:Validator", "VALIDATION FAILED: Dimension mismatch")
            return StickerConversionResult.ValidationFailed("Sticker dimensions must be exactly ${config.targetWidth}x${config.targetHeight} (Actual: ${width}x${height}).")
        }

        // Note: Frame duration and total duration validations for animated WebP are difficult 
        // to do in pure Android without a WebPDemuxer. We rely on the FFmpeg encoding pipeline 
        // stringently enforcing the fps/duration parameters at creation time.

        Log.i("Tel2What:Validator", "VALIDATION SUCCESS!")
        return StickerConversionResult.Success(
            outputFile = file,
            width = width,
            height = height,
            sizeBytes = sizeBytes,
            isAnimated = isAnimated
        )
    }
}
