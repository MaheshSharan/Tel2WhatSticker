package com.maheshsharan.tel2what.data.repository

import com.maheshsharan.tel2what.data.local.dao.StickerDao
import com.maheshsharan.tel2what.data.local.entity.StickerEntity
import com.maheshsharan.tel2what.data.local.entity.StickerPackEntity
import com.maheshsharan.tel2what.data.network.FileDownloader
import com.maheshsharan.tel2what.data.network.TelegramBotApi
import kotlinx.coroutines.flow.Flow

class StickerRepository(
    private val stickerDao: StickerDao,
    private val telegramBotApi: TelegramBotApi,
    private val fileDownloader: FileDownloader
) {

    fun getAllPacks(): Flow<List<StickerPackEntity>> {
        return stickerDao.getAllPacks()
    }
    
    suspend fun getPackById(packId: String): StickerPackEntity? {
        return stickerDao.getPackById(packId)
    }

    suspend fun fetchTelegramPackMetadata(packName: String): Result<com.maheshsharan.tel2what.data.network.model.TelegramStickerSet> = telegramBotApi.getStickerSet(packName)

    suspend fun fetchFilePath(fileId: String) = telegramBotApi.getFilePath(fileId)
    
    fun getDownloadUrl(filePath: String) = telegramBotApi.getDownloadUrl(filePath)
    
    suspend fun downloadBinary(url: String, destFile: java.io.File) = fileDownloader.downloadFile(url, destFile)

    suspend fun insertPack(pack: StickerPackEntity) {
        stickerDao.insertPack(pack)
    }

    suspend fun insertStickers(stickers: List<StickerEntity>) {
        stickerDao.insertStickers(stickers)
    }

    suspend fun updateStickerStatus(stickerId: Long, status: String) {
        stickerDao.updateStickerStatus(stickerId, status)
    }

    suspend fun getAllPacksSync(): List<StickerPackEntity> {
        return stickerDao.getAllPacksSync()
    }

    suspend fun deletePack(packId: String) {
        stickerDao.deletePack(packId)
    }

    suspend fun deleteStickersForPack(packId: String) {
        stickerDao.deleteStickersForPack(packId)
    }

    suspend fun getStickersForPackSync(packId: String): List<StickerEntity> {
        return stickerDao.getStickersForPackSync(packId)
    }
}
