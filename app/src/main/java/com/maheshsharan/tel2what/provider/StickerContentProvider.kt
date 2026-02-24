package com.maheshsharan.tel2what.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.maheshsharan.tel2what.data.local.AppDatabase
import com.maheshsharan.tel2what.data.local.entity.StickerEntity
import com.maheshsharan.tel2what.data.local.entity.StickerPackEntity
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileNotFoundException

class StickerContentProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.maheshsharan.tel2what.provider"
        const val MATCHER_METADATA = 1
        const val MATCHER_STICKERS = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "metadata", MATCHER_METADATA)
            addURI(AUTHORITY, "stickers/*", MATCHER_STICKERS)
        }
    }

    private lateinit var database: AppDatabase

    override fun onCreate(): Boolean {
        database = AppDatabase.getDatabase(context!!)
        return true
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? {
        val code = uriMatcher.match(uri)
        return when (code) {
            MATCHER_METADATA -> getPackMetadata()
            MATCHER_STICKERS -> getStickers(uri.lastPathSegment ?: "")
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    private fun getPackMetadata(): Cursor {
        val cursor = MatrixCursor(
            arrayOf(
                "identifier", "name", "publisher", "tray_image_file", "publisher_email",
                "publisher_website", "privacy_policy_website", "license_agreement_website",
                "image_data_version", "avoid_cache", "animated_sticker_pack"
            )
        )

        // Run blocking here is okay since WhatsApp queries the provider in a background thread of its own
        runBlocking {
            val packs = database.stickerDao().getAllPacksSync()
            for (pack in packs) {
                cursor.addRow(
                    arrayOf(
                        pack.identifier,
                        pack.name,
                        pack.publisher,
                        File(pack.trayImageFile).name,
                        pack.publisherEmail,
                        pack.publisherWebsite,
                        pack.privacyPolicyWebsite,
                        pack.licenseAgreementWebsite,
                        pack.imageDataVersion,
                        if (pack.avoidCache) 1 else 0,
                        if (pack.animatedStickerPack) 1 else 0
                    )
                )
            }
        }
        return cursor
    }

    private fun getStickers(identifier: String): Cursor {
        val cursor = MatrixCursor(arrayOf("image_file", "emojis"))

        runBlocking {
            val stickers = database.stickerDao().getStickersForPackSync(identifier)
            for (sticker in stickers) {
                if (sticker.status == "READY") {
                    cursor.addRow(arrayOf(File(sticker.imageFile).name, sticker.emojis))
                }
            }
        }
        return cursor
    }

    override fun getType(uri: Uri): String? {
        val code = uriMatcher.match(uri)
        return when (code) {
            MATCHER_METADATA -> "vnd.android.cursor.dir/vnd.$AUTHORITY.metadata"
            MATCHER_STICKERS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.stickers"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        val pathSegments = uri.pathSegments
        if (pathSegments.size != 3) {
            throw IllegalArgumentException("URI must have 3 segments: stickers/<pack_id>/<file_name>")
        }

        val packId = pathSegments[1]
        val fileName = pathSegments[2]

        return try {
            val file = getFileFromLocal(packId, fileName)
            if (file != null && file.exists()) {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileFromLocal(packId: String, fileName: String): File? {
        var result: File? = null
        runBlocking {
            // First check if it's the tray icon
            val pack = database.stickerDao().getPackById(packId)
            if (pack != null && File(pack.trayImageFile).name == fileName) {
                result = File(pack.trayImageFile)
                return@runBlocking
            }

            // Otherwise check stickers
            val stickers = database.stickerDao().getStickersForPackSync(packId)
            val sticker = stickers.find { File(it.imageFile).name == fileName }
            if (sticker != null) {
                result = File(sticker.imageFile)
            }
        }
        return result
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
