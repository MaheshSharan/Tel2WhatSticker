package com.maheshsharan.tel2what.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.maheshsharan.tel2what.data.local.entity.StickerEntity
import com.maheshsharan.tel2what.data.local.entity.StickerPackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StickerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPack(pack: StickerPackEntity)

    // Safe update: generates UPDATE...SET...WHERE, does NOT trigger ForeignKey CASCADE.
    // Use this for all modifications to an existing pack row that already has child sticker rows.
    @Update
    suspend fun updatePack(pack: StickerPackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStickers(stickers: List<StickerEntity>): LongArray

    @Query("SELECT * FROM sticker_packs")
    fun getAllPacks(): Flow<List<StickerPackEntity>>

    @Query("SELECT * FROM sticker_packs WHERE identifier = :packId LIMIT 1")
    suspend fun getPackById(packId: String): StickerPackEntity?

    @Query("SELECT * FROM sticker_packs")
    suspend fun getAllPacksSync(): List<StickerPackEntity>

    @Query("SELECT * FROM stickers WHERE packId = :packId")
    fun getStickersForPack(packId: String): Flow<List<StickerEntity>>

    @Query("SELECT * FROM stickers WHERE packId = :packId")
    suspend fun getStickersForPackSync(packId: String): List<StickerEntity>

    @Query("SELECT * FROM stickers WHERE packId = :packId AND status = 'READY' AND isSelected = 1")
    suspend fun getSelectedReadyStickersForPackSync(packId: String): List<StickerEntity>

    @Query("UPDATE stickers SET status = :status WHERE id = :stickerId")
    suspend fun updateStickerStatus(stickerId: Long, status: String)

    @Query("UPDATE stickers SET status = :status, imageFile = :imageFile WHERE id = :stickerId")
    suspend fun updateStickerStatusAndFile(stickerId: Long, status: String, imageFile: String)

    @Query("UPDATE stickers SET isSelected = 0 WHERE packId = :packId")
    suspend fun clearSelection(packId: String)

    @Query("UPDATE stickers SET isSelected = :isSelected WHERE id = :stickerId")
    suspend fun setStickerSelected(stickerId: Long, isSelected: Boolean)

    @Query("DELETE FROM sticker_packs WHERE identifier = :packId")
    suspend fun deletePack(packId: String)

    @Query("DELETE FROM sticker_packs")
    suspend fun deleteAllPacks()

    @Query("DELETE FROM stickers WHERE packId = :packId")
    suspend fun deleteStickersForPack(packId: String)
}
