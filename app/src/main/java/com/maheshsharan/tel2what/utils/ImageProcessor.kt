package com.maheshsharan.tel2what.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object ImageProcessor {

    private const val MAX_DIMENSION = 512f
    private const val MAX_FILE_SIZE_BYTES = 100 * 1024 // 100 KB limit for Stickers
    private const val MAX_TRAY_SIZE_BYTES = 50 * 1024 // 50 KB limit for Tray Icons
    private const val TRAY_DIMENSION = 96f

    /**
     * Resizes and compresses a static image into a WhatsApp valid WebP sticker format.
     * @param inputFile The downloaded raw image file.
     * @param outputFile The destination file for the optimized WebP sticker.
     */
    fun processStaticSticker(inputFile: File, outputFile: File): Boolean {
        try {
            val originalBitmap = BitmapFactory.decodeFile(inputFile.absolutePath) ?: return false

            // Calculate ratios for exactly fitting within 512x512
            val width = originalBitmap.width
            val height = originalBitmap.height
            val ratio = max(width / MAX_DIMENSION, height / MAX_DIMENSION)

            val newWidth = if (ratio > 1) (width / ratio).toInt() else width
            val newHeight = if (ratio > 1) (height / ratio).toInt() else height

            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            
            // For WhatsApp, dimensions must not exceed 512x512, and weight < 100kb
            // We use WEBP lossy compression and iterate to ensure file size constraints
            var quality = 100
            var success = false

            while (quality > 10) {
                val out = FileOutputStream(outputFile)
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                
                resizedBitmap.compress(format, quality, out)
                out.flush()
                out.close()

                if (outputFile.length() <= MAX_FILE_SIZE_BYTES) {
                    success = true
                    break
                }
                quality -= 10
            }

            originalBitmap.recycle()
            if (resizedBitmap != originalBitmap) {
                resizedBitmap.recycle()
            }

            return success
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Generates a 96x96 tray icon exactly as required by WhatsApp (<50kb).
     */
    fun processTrayIcon(inputFile: File, outputFile: File): Boolean {
        try {
            val originalBitmap = BitmapFactory.decodeFile(inputFile.absolutePath) ?: return false

            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, TRAY_DIMENSION.toInt(), TRAY_DIMENSION.toInt(), true)

            var quality = 100
            var success = false

            while (quality > 10) {
                val out = FileOutputStream(outputFile)
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                resizedBitmap.compress(format, quality, out)
                out.flush()
                out.close()

                if (outputFile.length() <= MAX_TRAY_SIZE_BYTES) {
                    success = true
                    break
                }
                quality -= 10
            }

            originalBitmap.recycle()
            if (resizedBitmap != originalBitmap) {
                resizedBitmap.recycle()
            }

            return success
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
