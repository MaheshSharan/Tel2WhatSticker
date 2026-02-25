package com.maheshsharan.tel2what.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

class StaticStickerConverter : StickerConverter {

    override suspend fun convert(
        inputFile: File,
        outputFile: File,
        config: ConversionConfig
    ): StickerConversionResult = withContext(Dispatchers.IO) {
        var originalBitmap: Bitmap? = null
        var scaledBitmap: Bitmap? = null
        var canvasBitmap: Bitmap? = null

        try {
            originalBitmap = BitmapFactory.decodeFile(inputFile.absolutePath)
                ?: return@withContext StickerConversionResult.Failed("Could not decode static input file.")

            val originalWidth = originalBitmap.width.toFloat()
            val originalHeight = originalBitmap.height.toFloat()
            
            // Calculate ratio to fit completely within target bounds
            val ratio = minOf(config.targetWidth / originalWidth, config.targetHeight / originalHeight)
            val scaledWidth = (originalWidth * ratio).toInt()
            val scaledHeight = (originalHeight * ratio).toInt()

            scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)

            // CRITICAL: Must be EXACTLY target bounds (512x512) with transparent padding
            canvasBitmap = Bitmap.createBitmap(config.targetWidth, config.targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(canvasBitmap)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            
            // Draw perfectly centered
            val left = (config.targetWidth - scaledWidth) / 2f
            val top = (config.targetHeight - scaledHeight) / 2f
            canvas.drawBitmap(scaledBitmap, left, top, null)

            // Iterative lossy compression loop for WebP to guarantee size compliance
            var quality = 100
            var success = false

            while (quality > 10) {
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }

                FileOutputStream(outputFile).use { out ->
                    canvasBitmap.compress(format, quality, out)
                    out.flush()
                }

                if (outputFile.length() <= config.maxStaticSizeBytes) {
                    success = true
                    break
                }
                quality -= 10
            }

            if (!success) {
                return@withContext StickerConversionResult.Failed("Could not compress static file to under ${config.maxStaticSizeBytes / 1024}KB.")
            }

            // Route through final validator
            return@withContext WhatsAppStickerValidator.validateOutput(
                file = outputFile,
                isAnimated = false,
                config = config
            )
            
        } catch (e: Exception) {
            return@withContext StickerConversionResult.Failed("Static conversion exception", e)
        } finally {
            originalBitmap?.recycle()
            if (scaledBitmap != originalBitmap) scaledBitmap?.recycle()
            canvasBitmap?.recycle()
        }
    }
}
