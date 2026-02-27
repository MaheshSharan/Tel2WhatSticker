package com.maheshsharan.tel2what.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.maheshsharan.tel2what.data.local.AppDatabase
import java.io.File

class StickerContentProvider : ContentProvider() {

    companion object {
        private const val TAG = "Tel2What"
        const val AUTHORITY = "com.maheshsharan.tel2what.provider"

        // ── URI path segments ─────────────────────────────────────────────────
        // Do NOT rename these — WhatsApp hardcodes these path names in its queries.
        private const val METADATA        = "metadata"
        private const val STICKERS        = "stickers"
        private const val STICKERS_ASSET  = "stickers_asset"

        // ── URI match codes ───────────────────────────────────────────────────
        private const val METADATA_CODE                 = 1  // metadata          (all packs)
        private const val METADATA_CODE_FOR_SINGLE_PACK = 2  // metadata/*        (one pack by id)
        private const val STICKERS_CODE                 = 3  // stickers/*        (sticker list for a pack)
        private const val STICKERS_ASSET_CODE           = 4  // stickers_asset/*/* (sticker file bytes)
        private const val STICKER_PACK_TRAY_ICON_CODE   = 5  // stickers_asset/*/* (tray icon bytes, same path)

        // ── Metadata cursor column names ──────────────────────────────────────
        // These exact string values are required by WhatsApp. Do NOT change them.
        const val STICKER_PACK_IDENTIFIER_IN_QUERY  = "sticker_pack_identifier"
        const val STICKER_PACK_NAME_IN_QUERY        = "sticker_pack_name"
        const val STICKER_PACK_PUBLISHER_IN_QUERY   = "sticker_pack_publisher"
        const val STICKER_PACK_ICON_IN_QUERY        = "sticker_pack_icon"
        const val ANDROID_APP_DOWNLOAD_LINK         = "android_play_store_link"
        const val IOS_APP_DOWNLOAD_LINK             = "ios_app_download_link"
        const val PUBLISHER_EMAIL                   = "sticker_pack_publisher_email"
        const val PUBLISHER_WEBSITE                 = "sticker_pack_publisher_website"
        const val PRIVACY_POLICY_WEBSITE            = "sticker_pack_privacy_policy_website"
        const val LICENSE_AGREEMENT_WEBSITE         = "sticker_pack_license_agreement_website"
        const val IMAGE_DATA_VERSION                = "image_data_version"
        const val AVOID_CACHE                       = "whatsapp_will_not_cache_stickers"
        const val ANIMATED_STICKER_PACK             = "animated_sticker_pack"

        // ── Sticker cursor column names ───────────────────────────────────────
        const val STICKER_FILE_NAME_IN_QUERY        = "sticker_file_name"
        const val STICKER_FILE_EMOJI_IN_QUERY       = "sticker_emoji"
        const val STICKER_FILE_ACCESSIBILITY_TEXT   = "sticker_accessibility_text"

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, METADATA,                         METADATA_CODE)
            addURI(AUTHORITY, "$METADATA/*",                    METADATA_CODE_FOR_SINGLE_PACK)
            addURI(AUTHORITY, "$STICKERS/*",                    STICKERS_CODE)
            // stickers_asset/{packId}/{fileName}  — used for both sticker files and tray icon
            addURI(AUTHORITY, "$STICKERS_ASSET/*/*",            STICKERS_ASSET_CODE)
        }
    }

    private lateinit var database: AppDatabase

    override fun onCreate(): Boolean {
        database = AppDatabase.getDatabase(context!!)
        return true
    }

    // ─────────────────────────────────────────────────────────────────────────
    // query(): routes metadata and sticker-list queries
    // ─────────────────────────────────────────────────────────────────────────
    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? {
        Log.i(TAG, "Provider:query uri=$uri")
        return when (uriMatcher.match(uri)) {
            METADATA_CODE                 -> getPackMetadata(null)
            METADATA_CODE_FOR_SINGLE_PACK -> getPackMetadata(uri.lastPathSegment)
            STICKERS_CODE                 -> getStickersForPack(uri.lastPathSegment ?: "")
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getPackMetadata(): returns pack rows.
    //   identifier==null  → all packs (METADATA_CODE)
    //   identifier!=null  → single pack filtered by identifier (METADATA_CODE_FOR_SINGLE_PACK)
    // ─────────────────────────────────────────────────────────────────────────
    private fun getPackMetadata(identifier: String?): Cursor {
        val cursor = MatrixCursor(
            arrayOf(
                STICKER_PACK_IDENTIFIER_IN_QUERY,
                STICKER_PACK_NAME_IN_QUERY,
                STICKER_PACK_PUBLISHER_IN_QUERY,
                STICKER_PACK_ICON_IN_QUERY,
                ANDROID_APP_DOWNLOAD_LINK,
                IOS_APP_DOWNLOAD_LINK,
                PUBLISHER_EMAIL,
                PUBLISHER_WEBSITE,
                PRIVACY_POLICY_WEBSITE,
                LICENSE_AGREEMENT_WEBSITE,
                IMAGE_DATA_VERSION,
                AVOID_CACHE,
                ANIMATED_STICKER_PACK
            )
        )

        // Use synchronous DAO call directly - no runBlocking needed
        val packs = database.stickerDao().getAllPacksSyncBlocking()
        val filtered = if (identifier != null) packs.filter { it.identifier == identifier } else packs
        for (pack in filtered) {
            Log.i(TAG, "Provider:packMetadata id=${pack.identifier} tray=${pack.trayImageFile}")
            cursor.addRow(
                arrayOf(
                    pack.identifier,
                    pack.name,
                    pack.publisher,
                    // WhatsApp expects just the filename, not the full path
                    File(pack.trayImageFile).name,
                    "",   // android_play_store_link — not applicable, leave empty
                    "",   // ios_app_download_link   — not applicable, leave empty
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
        cursor.setNotificationUri(context!!.contentResolver, Uri.parse("content://$AUTHORITY/$METADATA"))
        return cursor
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getStickersForPack(): returns sticker rows for one pack (STICKERS_CODE)
    // ─────────────────────────────────────────────────────────────────────────
    private fun getStickersForPack(identifier: String): Cursor {
        val cursor = MatrixCursor(
            arrayOf(
                STICKER_FILE_NAME_IN_QUERY,
                STICKER_FILE_EMOJI_IN_QUERY,
                STICKER_FILE_ACCESSIBILITY_TEXT
            )
        )

        // Use synchronous DAO call directly - no runBlocking needed
        val stickers = database.stickerDao().getSelectedReadyStickersForPackSyncBlocking(identifier)
        Log.i(TAG, "Provider:getStickers id=$identifier count=${stickers.size}")
        for (sticker in stickers) {
            cursor.addRow(
                arrayOf(
                    File(sticker.imageFile).name,
                    sticker.emojis,           // comma-separated, WhatsApp splits on ","
                    sticker.accessibilityText
                )
            )
        }
        cursor.setNotificationUri(context!!.contentResolver, Uri.parse("content://$AUTHORITY/$STICKERS/$identifier"))
        return cursor
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getType(): MIME types — exact strings required by WhatsApp
    // ─────────────────────────────────────────────────────────────────────────
    override fun getType(uri: Uri): String {
        return when (uriMatcher.match(uri)) {
            METADATA_CODE                 -> "vnd.android.cursor.dir/vnd.$AUTHORITY.$METADATA"
            METADATA_CODE_FOR_SINGLE_PACK -> "vnd.android.cursor.item/vnd.$AUTHORITY.$METADATA"
            STICKERS_CODE                 -> "vnd.android.cursor.dir/vnd.$AUTHORITY.$STICKERS"
            STICKERS_ASSET_CODE           -> "image/webp"
            STICKER_PACK_TRAY_ICON_CODE   -> "image/webp"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // openAssetFile(): serves sticker and tray icon bytes.
    //
    // WhatsApp fetches files via:
    //   content://{authority}/stickers_asset/{packId}/{fileName}
    //
    // The path has 3 segments: ["stickers_asset", packId, fileName]
    // ─────────────────────────────────────────────────────────────────────────
    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        val segments = uri.pathSegments
        Log.i(TAG, "Provider:openAssetFile uri=$uri segments=${segments.size}")

        if (segments.size != 3) {
            Log.e(TAG, "Provider:openAssetFile wrong segment count=${segments.size} uri=$uri")
            return null
        }

        val packId   = segments[1]
        val fileName = segments[2]

        // Use synchronous resolution - no runBlocking needed
        val file = resolveFileBlocking(packId, fileName)
        return if (file != null && file.exists()) {
            Log.i(TAG, "Provider:openAssetFile serving file=${file.absolutePath}")
            try {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
            } catch (e: Exception) {
                Log.e(TAG, "Provider:openAssetFile failed to open file", e)
                null
            }
        } else {
            Log.e(TAG, "Provider:openAssetFile file not found packId=$packId fileName=$fileName")
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // resolveFileBlocking(): looks up a file by pack + filename from the DB.
    //   Checks tray icon first, then individual stickers.
    //   Uses synchronous DAO calls - safe for ContentProvider context.
    // ─────────────────────────────────────────────────────────────────────────
    private fun resolveFileBlocking(packId: String, fileName: String): File? {
        // 1. Check tray icon
        val pack = database.stickerDao().getPackByIdBlocking(packId)
        if (pack != null && File(pack.trayImageFile).name == fileName) {
            return File(pack.trayImageFile)
        }

        // 2. Check sticker files (all, not just selected — so tray icon alt paths also work)
        val stickers = database.stickerDao().getStickersForPackSyncBlocking(packId)
        return stickers
            .map { File(it.imageFile) }
            .firstOrNull { it.name == fileName }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Unsupported mutations
    // ─────────────────────────────────────────────────────────────────────────
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
