package com.maheshsharan.tel2what.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stickers",
    foreignKeys = [
        ForeignKey(
            entity = StickerPackEntity::class,
            parentColumns = ["identifier"],
            childColumns = ["packId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["packId"])]
)
data class StickerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packId: String,
    val imageFile: String,
    val emojis: String, // Comma-separated emojis
    val accessibilityText: String,
    val status: String, // DOWNLOADING, CONVERTING, READY, FAILED
    val isSelected: Boolean = false
)
