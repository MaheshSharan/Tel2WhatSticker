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

            val originalWidth = originalBitmap.width.toFloat()
            val originalHeight = originalBitmap.height.toFloat()
            val ratio = minOf(MAX_DIMENSION / originalWidth, MAX_DIMENSION / originalHeight)

            val scaledWidth = (originalWidth * ratio).toInt()
            val scaledHeight = (originalHeight * ratio).toInt()

            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
            
            // CRITICAL WA SPEC: Must be EXACTLY 512x512 with transparent padding
            val canvasBitmap = Bitmap.createBitmap(MAX_DIMENSION.toInt(), MAX_DIMENSION.toInt(), Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(canvasBitmap)
            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            
            // Draw centered
            val left = (MAX_DIMENSION - scaledWidth) / 2f
            val top = (MAX_DIMENSION - scaledHeight) / 2f
            canvas.drawBitmap(scaledBitmap, left, top, null)
            
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
                
                canvasBitmap.compress(format, quality, out)
                out.flush()
                out.close()

                if (outputFile.length() <= MAX_FILE_SIZE_BYTES) {
                    success = true
                    break
                }
                quality -= 10
            }

            originalBitmap.recycle()
            if (scaledBitmap != originalBitmap) scaledBitmap.recycle()
            canvasBitmap.recycle()

            if (success) {
                val finalOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(outputFile.absolutePath, finalOptions)
                android.util.Log.i(
                    "Tel2What", 
                    "ImageProcessor:processStaticSticker SUCCESS size=${outputFile.length() / 1024}KB dimen=${finalOptions.outWidth}x${finalOptions.outHeight} file=${outputFile.name}"
                )
            } else {
                android.util.Log.e("Tel2What", "ImageProcessor:processStaticSticker FAILED file=${outputFile.name}")
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

            if (success) {
                val finalOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(outputFile.absolutePath, finalOptions)
                android.util.Log.i(
                    "Tel2What", 
                    "ImageProcessor:processTrayIcon SUCCESS size=${outputFile.length() / 1024}KB dimen=${finalOptions.outWidth}x${finalOptions.outHeight} file=${outputFile.name}"
                )
            } else {
                android.util.Log.e("Tel2What", "ImageProcessor:processTrayIcon FAILED file=${outputFile.name}")
            }

            return success
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
