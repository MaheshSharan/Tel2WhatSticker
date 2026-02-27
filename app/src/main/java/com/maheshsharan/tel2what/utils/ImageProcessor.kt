package com.maheshsharan.tel2what.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import java.io.File
import java.io.FileOutputStream

object ImageProcessor {

    private const val MAX_TRAY_SIZE_BYTES = 50 * 1024 // 50 KB limit for Tray Icons
    private const val TRAY_DIMENSION = 96f

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
