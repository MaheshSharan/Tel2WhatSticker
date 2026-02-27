package com.maheshsharan.tel2what.engine.decoder

import android.util.Log
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

/**
 * Validates and decompresses a Telegram `.tgs` (GZIP JSON) file
 * and attempts to parse it into a LottieComposition.
 */
object TgsDecoder {

    private const val TAG = "Tel2What:TgsDecoder"

    suspend fun decode(tgsFile: File, cacheDir: File): LottieComposition? = withContext(Dispatchers.IO) {
        Log.i(TAG, "TgsDecoder: Starting decode of ${tgsFile.name}")
        Log.i(TAG, "TgsDecoder: File exists=${tgsFile.exists()}, size=${tgsFile.length()} bytes")
        
        if (!tgsFile.exists()) {
            Log.e(TAG, "TgsDecoder: ERROR - TGS file does not exist!")
            return@withContext null
        }
        
        if (tgsFile.length() == 0L) {
            Log.e(TAG, "TgsDecoder: ERROR - TGS file is empty!")
            return@withContext null
        }
        
        val workingDir = File(cacheDir, "tgs_decode_${System.currentTimeMillis()}")
        var jsonFile: File?

        try {
            workingDir.mkdirs()
            jsonFile = File(workingDir, "animation.json")
            
            Log.i(TAG, "TgsDecoder: Decompressing GZIP to ${jsonFile.absolutePath}")
            
            // Step 1: Decompress GZIP
            decompressGzip(tgsFile, jsonFile)
            
            Log.i(TAG, "TgsDecoder: GZIP decompressed, JSON size=${jsonFile.length()} bytes")

            // Step 2: Validate JSON integrity and load Composition synchronously
            Log.i(TAG, "TgsDecoder: Parsing Lottie composition from JSON")
            val lottieResult = LottieCompositionFactory.fromJsonInputStreamSync(
                FileInputStream(jsonFile), "cache_${jsonFile.name}"
            )

            if (lottieResult.exception != null) {
                Log.e(TAG, "TgsDecoder: Lottie composition failed to parse.", lottieResult.exception)
                return@withContext null
            }

            val composition = lottieResult.value
            if (composition != null) {
                Log.i(TAG, "TgsDecoder: SUCCESS - Composition parsed, duration=${composition.duration}ms, bounds=${composition.bounds}")
            } else {
                Log.e(TAG, "TgsDecoder: ERROR - Composition is null despite no exception")
            }

            return@withContext composition
        } catch (e: Exception) {
            Log.e(TAG, "TgsDecoder: Exception during decompression/parsing - ${e.message}", e)
            return@withContext null
        } finally {
            // Cleanup intermediary unzipped JSON immediately to preserve storage
            try {
                workingDir.deleteRecursively()
                Log.d(TAG, "TgsDecoder: Cleaned up working directory")
            } catch (e: Exception) {
                Log.e(TAG, "TgsDecoder: Failed to cleanup TGS working dir.", e)
            }
        }
    }

    private fun decompressGzip(input: File, output: File) {
        GZIPInputStream(FileInputStream(input)).use { gis ->
            FileOutputStream(output).use { fos ->
                val buffer = ByteArray(4096)
                var len: Int
                while (gis.read(buffer).also { len = it } > 0) {
                    fos.write(buffer, 0, len)
                }
            }
        }
    }
}
