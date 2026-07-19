package com.maheshsharan.tel2what.data.network.model

import org.json.JSONObject

data class PhotoSize(
    val fileId: String,
    val fileUniqueId: String,
    val fileSize: Long,
    val width: Int,
    val height: Int
)

data class TelegramSticker(
    val fileId: String,
    val fileUniqueId: String,
    val width: Int,
    val height: Int,
    val isAnimated: Boolean,
    val isVideo: Boolean,
    val emoji: String? = null,
    val thumbnail: PhotoSize? = null,
    val thumb: PhotoSize? = null
)

data class TelegramStickerSet(
    val name: String,
    val title: String,
    val stickerType: String,
    val stickers: List<TelegramSticker>,
    val thumbnail: PhotoSize? = null,
    val thumb: PhotoSize? = null
)

val TelegramStickerSet.previewFileId: String?
    get() = thumbnail?.fileId 
        ?: thumb?.fileId 
        ?: stickers.firstOrNull()?.thumbnail?.fileId
        ?: stickers.firstOrNull()?.thumb?.fileId
        ?: stickers.firstOrNull()?.fileId

object TelegramResponseParser {

    private fun parsePhotoSize(obj: JSONObject?): PhotoSize? {
        if (obj == null) return null
        return PhotoSize(
            fileId = obj.optString("file_id"),
            fileUniqueId = obj.optString("file_unique_id"),
            fileSize = obj.optLong("file_size", 0),
            width = obj.optInt("width", 0),
            height = obj.optInt("height", 0)
        )
    }

    fun parseStickerSet(jsonString: String): TelegramStickerSet? {
        try {
            val root = JSONObject(jsonString)
            if (!root.optBoolean("ok", false)) return null

            val result = root.getJSONObject("result")
            val name = result.optString("name")
            val title = result.optString("title")
            val stickerType = result.optString("sticker_type", "regular")
            val thumbnail = parsePhotoSize(result.optJSONObject("thumbnail"))
            val thumb = parsePhotoSize(result.optJSONObject("thumb"))
            
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
                            emoji = stickerObj.optString("emoji"),
                            thumbnail = parsePhotoSize(stickerObj.optJSONObject("thumbnail")),
                            thumb = parsePhotoSize(stickerObj.optJSONObject("thumb"))
                        )
                    )
                }
            }

            return TelegramStickerSet(name, title, stickerType, stickersList, thumbnail, thumb)

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
