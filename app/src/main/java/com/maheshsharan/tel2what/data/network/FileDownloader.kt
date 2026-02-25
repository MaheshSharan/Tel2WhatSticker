package com.maheshsharan.tel2what.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class FileDownloader {

    private val client = OkHttpClient()

    /**
     * Downloads a file from the provided URL to the specific destination File.
     * @param url The direct download URL.
     * @param destFile The destination file where binary stream will be saved.
     * @return Boolean true if successful, false otherwise.
     */
    suspend fun downloadFile(url: String, destFile: File): Boolean = withContext(Dispatchers.IO) {
        Log.i("Tel2What:FileDownloader", "Starting download")
        Log.i("Tel2What:FileDownloader", "URL: $url")
        Log.i("Tel2What:FileDownloader", "Destination: ${destFile.absolutePath}")
        
        val request = Request.Builder()
            .url(url)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                Log.i("Tel2What:FileDownloader", "Response code: ${response.code}")
                Log.i("Tel2What:FileDownloader", "Response successful: ${response.isSuccessful}")
                
                if (!response.isSuccessful) {
                    Log.e("Tel2What:FileDownloader", "Download failed with HTTP ${response.code}: ${response.message}")
                    return@withContext false
                }
                
                val body = response.body
                if (body == null) {
                    Log.e("Tel2What:FileDownloader", "Response body is null")
                    return@withContext false
                }
                
                val contentLength = body.contentLength()
                Log.i("Tel2What:FileDownloader", "Content length: $contentLength bytes")
                
                // Ensure parent directories exist
                destFile.parentFile?.mkdirs()
                
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(destFile)
                
                inputStream.use { input ->
                    outputStream.use { output ->
                        val bytesWritten = input.copyTo(output)
                        Log.i("Tel2What:FileDownloader", "Bytes written: $bytesWritten")
                    }
                }
                
                Log.i("Tel2What:FileDownloader", "Download complete. File size: ${destFile.length()} bytes")
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e("Tel2What:FileDownloader", "Download exception: ${e.message}", e)
            e.printStackTrace()
            // Cleanup incomplete file if any error occurs
            if (destFile.exists()) {
                destFile.delete()
                Log.i("Tel2What:FileDownloader", "Cleaned up incomplete file")
            }
            return@withContext false
        }
    }
}
