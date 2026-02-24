package com.maheshsharan.tel2what.data.network

import com.maheshsharan.tel2what.data.network.model.TelegramResponseParser
import com.maheshsharan.tel2what.data.network.model.TelegramStickerSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class TelegramBotApi {

    // Temporarily hardcoded for testing as requested
    private val botToken = "8222863145:AAFBPD95I9qNsKXaxhmiT0lMOxqMecbwdi4"
    private val client = OkHttpClient()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getStickerSet(packName: String): Result<TelegramStickerSet> = withContext(Dispatchers.IO) {
        val url = "https://api.telegram.org/bot$botToken/getStickerSet"
        
        val jsonBody = JSONObject().apply {
            put("name", packName)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.code == 401 || response.code == 404) {
                    if (response.code == 401) {
                        return@withContext Result.failure(Exception("Invalid Telegram Bot Token. Please configure a valid token in your local.properties file."))
                    } else if (response.code == 404 && response.message.contains("Not Found", ignoreCase = true)) {
                        // Sometimes telegram returns 404 Not Found for bad tokens if the URL path mismatches.
                        return@withContext Result.failure(Exception("Bot endpoint not found. Your Bot Token might be invalid or restricted."))
                    }
                }

                val bodyStr = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext if (response.code == 400) {
                        if (bodyStr.contains("sticker set not found", ignoreCase = true)) {
                            Result.failure(Exception("Sticker pack not found. Make sure the Telegram link is correct and the pack is public."))
                        } else {
                            val desc = try { JSONObject(bodyStr).getString("description") } catch (e: Exception) { "Bad Request" }
                            Result.failure(Exception("Telegram API Error: $desc"))
                        }
                    } else {
                        Result.failure(Exception("Telegram API error (HTTP ${response.code})"))
                    }
                }
                
                val parsed = TelegramResponseParser.parseStickerSet(bodyStr)
                if (parsed != null) {
                    return@withContext Result.success(parsed)
                } else {
                    return@withContext Result.failure(Exception("Failed to parse the sticker pack data from Telegram."))
                }
            }
        } catch (e: java.net.UnknownHostException) {
            return@withContext Result.failure(Exception("No internet connection. Please check your network and try again."))
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(Exception("Network error: ${e.localizedMessage}"))
        }
    }

    suspend fun getFilePath(fileId: String): String? = withContext(Dispatchers.IO) {
        val url = "https://api.telegram.org/bot$botToken/getFile?file_id=$fileId"
        
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                return@withContext TelegramResponseParser.parseFilePath(bodyStr)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
    
    fun getDownloadUrl(filePath: String): String {
        return "https://api.telegram.org/file/bot$botToken/$filePath"
    }
}
