package com.maheshsharan.tel2what.data.network.model

import org.json.JSONObject

data class TelegramSticker(
    val fileId: String,
    val fileUniqueId: String,
    val width: Int,
    val height: Int,
    val isAnimated: Boolean,
    val isVideo: Boolean,
    val emoji: String? = null
)

data class TelegramStickerSet(
    val name: String,
    val title: String,
    val stickerType: String,
    val stickers: List<TelegramSticker>
)

object TelegramResponseParser {

    fun parseStickerSet(jsonString: String): TelegramStickerSet? {
        try {
            val root = JSONObject(jsonString)
            if (!root.optBoolean("ok", false)) return null

            val result = root.getJSONObject("result")
            val name = result.optString("name")
            val title = result.optString("title")
            val stickerType = result.optString("sticker_type", "regular")
            
            val stickersArray = result.optJSONArray("stickers")
            val stickersList = mutableListOf<TelegramSticker>()

            if (stickersArray != null) {
                for (i in 0 until stickersArray.length()) {
                    val stickerObj = stickersArray.getJSONObject(i)
                    stickersList.add(
                        TelegramSticker(
                            fileId = stickerObj.optString("file_id"),
                            fileUniqueId = stickerObj.optString("file_unique_id"),
                            width = stickerObj.optInt("width", 512),
                            height = stickerObj.optInt("height", 512),
                            isAnimated = stickerObj.optBoolean("is_animated", false),
                            isVideo = stickerObj.optBoolean("is_video", false),
                            emoji = stickerObj.optString("emoji")
                        )
                    )
                }
            }

            return TelegramStickerSet(name, title, stickerType, stickersList)

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun parseFilePath(jsonString: String): String? {
        try {
            val root = JSONObject(jsonString)
            if (!root.optBoolean("ok", false)) return null

            val result = root.getJSONObject("result")
            return result.optString("file_path")
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
