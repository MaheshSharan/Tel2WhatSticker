package com.maheshsharan.tel2what.data.network

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
        val request = Request.Builder()
            .url(url)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                
                val body = response.body ?: return@withContext false
                
                // Ensure parent directories exist
                destFile.parentFile?.mkdirs()
                
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(destFile)
                
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Cleanup incomplete file if any error occurs
            if (destFile.exists()) {
                destFile.delete()
            }
            return@withContext false
        }
    }
}
