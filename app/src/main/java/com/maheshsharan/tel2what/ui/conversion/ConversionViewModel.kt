package com.maheshsharan.tel2what.ui.conversion

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maheshsharan.tel2what.data.local.entity.StickerEntity
import com.maheshsharan.tel2what.data.local.entity.StickerPackEntity
import com.maheshsharan.tel2what.data.network.model.TelegramSticker
import com.maheshsharan.tel2what.data.repository.StickerRepository
import com.maheshsharan.tel2what.utils.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ConversionProgress(
    val downloaded: Int = 0,
    val totalToDownload: Int = 0,
    val isFinished: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String = ""
)

class ConversionViewModel(
    private val repository: StickerRepository,
    private val context: Context
) : ViewModel() {

    private val _progressData = MutableStateFlow(ConversionProgress())
    val progressData: StateFlow<ConversionProgress> = _progressData.asStateFlow()

    private val _stickers = MutableStateFlow<List<StickerEntity>>(emptyList())
    val stickers: StateFlow<List<StickerEntity>> = _stickers.asStateFlow()

    private var currentIndex = 0
    private var allTelegramStickers = listOf<TelegramSticker>()
    private var currentPackTitle = ""
    private var currentPackName = ""

    fun initAndStart(packName: String, packTitle: String) {
        if (currentPackName == packName) return // already initialized

        currentPackName = packName
        currentPackTitle = packTitle
        currentIndex = 0

        viewModelScope.launch {
            val result = repository.fetchTelegramPackMetadata(packName)
            if (result.isSuccess) {
                val set = result.getOrNull()
                if (set != null) {
                    allTelegramStickers = set.stickers
                    // Save empty pack to DB first
                    repository.insertPack(
                        StickerPackEntity(
                            identifier = packName,
                            name = packTitle,
                            publisher = "Telegram",
                            trayImageFile = "", // will be set later
                            publisherEmail = "",
                            publisherWebsite = "",
                            privacyPolicyWebsite = "",
                            licenseAgreementWebsite = "",
                            animatedStickerPack = false, // V1 handles static webp best, will compress tgs if possible later
                            imageDataVersion = "1",
                            avoidCache = false,
                            sizeBytes = 0,
                            dateAdded = System.currentTimeMillis()
                        )
                    )

                    downloadNextBatch()
                }
            } else {
                _progressData.value = _progressData.value.copy(
                    isError = true,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to fetch pack details."
                )
            }
        }
    }

    fun downloadNextBatch() {
        val remaining = allTelegramStickers.size - currentIndex
        if (remaining <= 0) {
            _progressData.value = _progressData.value.copy(isFinished = true)
            return
        }

        val limit = if (remaining > 30) 30 else remaining
        val batch = allTelegramStickers.subList(currentIndex, currentIndex + limit)
        currentIndex += limit

        _progressData.value = _progressData.value.copy(
            downloaded = 0,
            totalToDownload = batch.size,
            isFinished = false,
            isError = false
        )

        // Initialize state UI array
        val initialStickers = batch.map {
            StickerEntity(
                packId = currentPackName,
                imageFile = "",
                emojis = it.emoji ?: "😀",
                accessibilityText = "Sticker",
                status = "DOWNLOADING"
            )
        }
        _stickers.value = _stickers.value + initialStickers
        val startIndexForThisBatch = _stickers.value.size - initialStickers.size

        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = File(context.cacheDir, "telegram/$currentPackName")
            cacheDir.mkdirs()
            val finalDir = File(context.filesDir, "packs/$currentPackName")
            finalDir.mkdirs()

            var downloadedCount = 0
            
            // Limit concurrency to 4 max simultaneous downloads/conversions to prevent memory spikes
            val concurrencySemaphore = kotlinx.coroutines.sync.Semaphore(4)

            val deferreds = batch.mapIndexed { index, telegramSticker ->
                async {
                    concurrencySemaphore.acquire()
                    val globalIndex = startIndexForThisBatch + index
                    
                    try {
                        // 1. Get File Path
                        val filePathInfo = repository.fetchFilePath(telegramSticker.fileId)
                        if (filePathInfo == null) {
                            updateStickerStatus(globalIndex, "FAILED")
                            return@async
                        }

                        // 2. Download Binary
                        val downloadUrl = repository.getDownloadUrl(filePathInfo)
                        val cacheFile = File(cacheDir, "${telegramSticker.fileUniqueId}_raw")
                        val downloadSuccess = repository.downloadBinary(downloadUrl, cacheFile)

                        if (!downloadSuccess) {
                            updateStickerStatus(globalIndex, "FAILED")
                            return@async
                        }

                        updateStickerStatus(globalIndex, "CONVERTING")

                        // 3. Process to WebP 512x512
                        val finalFile = File(finalDir, "${telegramSticker.fileUniqueId}.webp")
                        val processSuccess = ImageProcessor.processStaticSticker(cacheFile, finalFile)

                        if (processSuccess) {
                            // First completed sticker becomes tray icon if pack doesn't have one
                            if (globalIndex == 0) {
                                val trayFile = File(finalDir, "tray.webp")
                                ImageProcessor.processTrayIcon(finalFile, trayFile)
                                val currentPack = repository.getPackById(currentPackName)
                                if (currentPack != null) {
                                    repository.insertPack(currentPack.copy(trayImageFile = trayFile.absolutePath))
                                }
                            }
                            
                            updateStickerStatus(globalIndex, "READY", finalFile.absolutePath)
                        } else {
                            updateStickerStatus(globalIndex, "FAILED")
                        }

                        // Clear the raw cache file immediately to save disk/memory
                        if (cacheFile.exists()) {
                            cacheFile.delete()
                        }

                    } catch (e: Exception) {
                        updateStickerStatus(globalIndex, "FAILED")
                    } finally {
                        downloadedCount++
                        _progressData.value = _progressData.value.copy(downloaded = downloadedCount)
                        concurrencySemaphore.release()
                    }
                }
            }

            // Wait for all in this batch to finish
            deferreds.awaitAll()

            // Save all to DB
            repository.insertStickers(_stickers.value)

            _progressData.value = _progressData.value.copy(isFinished = true)
        }
    }

    private fun updateStickerStatus(index: Int, status: String, imagePath: String = "") {
        val currentList = _stickers.value.toMutableList()
        val item = currentList[index]
        currentList[index] = item.copy(status = status, imageFile = imagePath.ifEmpty { item.imageFile })
        _stickers.value = currentList
    }
}

class ConversionViewModelFactory(
    private val repository: StickerRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConversionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConversionViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
