package com.maheshsharan.tel2what.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sticker_packs")
data class StickerPackEntity(
    @PrimaryKey val identifier: String,
    val name: String,
    val publisher: String,
    val trayImageFile: String,
    val publisherEmail: String,
    val publisherWebsite: String,
    val privacyPolicyWebsite: String,
    val licenseAgreementWebsite: String,
    val animatedStickerPack: Boolean,
    val imageDataVersion: String,
    val avoidCache: Boolean,
    val sizeBytes: Long,
    val dateAdded: Long
)
