package com.maheshsharan.tel2what.ui.conversion

import android.content.Context
import android.util.Log
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
import java.util.concurrent.atomic.AtomicInteger

data class ConversionProgress(
    val overallCompleted: Int = 0,
    val overallTotal: Int = 0,
    val batchCompleted: Int = 0,
    val batchTotal: Int = 0,
    val readyCount: Int = 0,
    val speedStickersPerSec: Double = 0.0,
    val etaSeconds: Long? = null,
    val isBatchFinished: Boolean = false,
    val isAllFinished: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String = ""
)

class ConversionViewModel(
    private val repository: StickerRepository,
    private val context: Context
) : ViewModel() {

    private companion object {
        private const val TAG = "Tel2What"
    }

    private val _progressData = MutableStateFlow(ConversionProgress())
    val progressData: StateFlow<ConversionProgress> = _progressData.asStateFlow()

    private val _stickers = MutableStateFlow<List<StickerEntity>>(emptyList())
    val stickers: StateFlow<List<StickerEntity>> = _stickers.asStateFlow()

    private var currentIndex = 0
    private var allTelegramStickers = listOf<TelegramSticker>()
    private var currentPackTitle = ""
    private var currentPackName = ""

    private var overallStartTimeMillis: Long = 0L
    private var batchStartTimeMillis: Long = 0L

    fun initAndStart(packName: String, packTitle: String) {
        Log.i(TAG, "Conversion:initAndStart packName=$packName packTitle=$packTitle")
        if (currentPackName == packName) return

        currentPackName = packName
        currentPackTitle = packTitle
        currentIndex = 0
        allTelegramStickers = emptyList()
        _stickers.value = emptyList()
        overallStartTimeMillis = System.currentTimeMillis()
        _progressData.value = ConversionProgress()

        viewModelScope.launch {
            Log.i(TAG, "Conversion:fetchMetadata start pack=$packName")
            val result = repository.fetchTelegramPackMetadata(packName)
            Log.i(TAG, "Conversion:fetchMetadata done pack=$packName isSuccess=${result.isSuccess}")
            if (result.isSuccess) {
                val set = result.getOrNull()
                if (set != null) {
                    allTelegramStickers = set.stickers
                    Log.i(TAG, "Conversion:metadata ok pack=$packName total=${allTelegramStickers.size} type=${set.stickerType}")

                    _progressData.value = _progressData.value.copy(
                        overallTotal = allTelegramStickers.size,
                        overallCompleted = 0,
                        batchTotal = 0,
                        batchCompleted = 0,
                        readyCount = 0,
                        isBatchFinished = false,
                        isAllFinished = false,
                        isError = false,
                        errorMessage = ""
                    )

                    Log.i(TAG, "Conversion:insertPack identifier=$packName")
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

                    Log.i(TAG, "Conversion:startFirstBatch pack=$packName")
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
        Log.i(TAG, "Conversion:downloadNextBatch pack=$currentPackName currentIndex=$currentIndex remaining=$remaining")
        if (remaining <= 0) {
            _progressData.value = _progressData.value.copy(
                isBatchFinished = true,
                isAllFinished = true
            )
            return
        }

        val limit = minOf(30, remaining)
        val batch = allTelegramStickers.subList(currentIndex, currentIndex + limit)
        Log.i(TAG, "Conversion:batchCreated pack=$currentPackName batchSize=$limit startAt=$currentIndex")
        currentIndex += limit

        val startIndexForThisBatch = _stickers.value.size
        batchStartTimeMillis = System.currentTimeMillis()

        val placeholders = batch.map {
            StickerEntity(
                packId = currentPackName,
                imageFile = "",
                emojis = it.emoji ?: "😀",
                accessibilityText = "Sticker",
                status = "DOWNLOADING",
                isSelected = false
            )
        }

        _stickers.value = _stickers.value + placeholders

        _progressData.value = _progressData.value.copy(
            overallTotal = allTelegramStickers.size,
            batchCompleted = 0,
            batchTotal = placeholders.size,
            isBatchFinished = false,
            isAllFinished = false,
            isError = false,
            errorMessage = ""
        )

        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = File(context.cacheDir, "telegram/$currentPackName")
            cacheDir.mkdirs()
            val finalDir = File(context.filesDir, "packs/$currentPackName")
            finalDir.mkdirs()

            try {
                Log.i(TAG, "Conversion:insertStickers start pack=$currentPackName placeholders=${placeholders.size}")
                val insertedIds = repository.insertStickers(placeholders)
                Log.i(TAG, "Conversion:insertStickers done pack=$currentPackName insertedIds=${insertedIds.size} firstId=${insertedIds.firstOrNull()}")
                applyInsertedIds(startIndexForThisBatch, insertedIds)

                val dbCountAfterInsert = repository.getStickersForPackSync(currentPackName).size
                Log.i(TAG, "Conversion:dbCount afterInsert pack=$currentPackName count=$dbCountAfterInsert")

            } catch (e: Exception) {
                Log.e(TAG, "Conversion:insertStickers failed pack=$currentPackName", e)
                throw e
            }

            val batchCompletedCounter = AtomicInteger(0)
            val overallCompletedCounter = AtomicInteger(_stickers.value.count { it.status == "READY" || it.status == "FAILED" })

            val concurrencySemaphore = kotlinx.coroutines.sync.Semaphore(4)

            val deferreds = batch.mapIndexed { index, telegramSticker ->
                async {
                    concurrencySemaphore.acquire()
                    val globalIndex = startIndexForThisBatch + index
                    val stickerId = getStickerId(globalIndex)

                    Log.i(TAG, "Conversion:stickerStart idx=$index globalIndex=$globalIndex id=$stickerId")

                    try {
                        val filePathInfo = repository.fetchFilePath(telegramSticker.fileId)
                        if (filePathInfo == null) {
                            Log.i(TAG, "Conversion:stickerFail idx=$index id=$stickerId reason=filePathNull")
                            markFailed(globalIndex, stickerId)
                            return@async
                        }

                        val downloadUrl = repository.getDownloadUrl(filePathInfo)
                        val cacheFile = File(cacheDir, "${telegramSticker.fileUniqueId}_raw")
                        val downloadSuccess = repository.downloadBinary(downloadUrl, cacheFile)

                        if (!downloadSuccess) {
                            Log.i(TAG, "Conversion:stickerFail idx=$index id=$stickerId reason=download")
                            markFailed(globalIndex, stickerId)
                            return@async
                        }

                        setStatus(globalIndex, stickerId, "CONVERTING")

                        val finalFile = File(finalDir, "${telegramSticker.fileUniqueId}.webp")
                        val processSuccess = ImageProcessor.processStaticSticker(cacheFile, finalFile)

                        if (processSuccess) {
                            ensureTrayIconIfMissing(finalDir, finalFile)
                            Log.i(TAG, "Conversion:stickerReady idx=$index id=$stickerId file=${finalFile.name}")
                            markReady(globalIndex, stickerId, finalFile.absolutePath)
                        } else {
                            Log.i(TAG, "Conversion:stickerFail idx=$index id=$stickerId reason=process")
                            markFailed(globalIndex, stickerId)
                        }

                        if (cacheFile.exists()) {
                            cacheFile.delete()
                        }

                    } catch (e: Exception) {
                        markFailed(globalIndex, stickerId)
                    } finally {
                        val batchDone = batchCompletedCounter.incrementAndGet()
                        val overallDone = overallCompletedCounter.incrementAndGet()
                        updateProgress(batchDone, placeholders.size, overallDone, allTelegramStickers.size)
                        concurrencySemaphore.release()
                    }
                }
            }

            deferreds.awaitAll()

            val dbStickers = repository.getStickersForPackSync(currentPackName)
            val dbReady = dbStickers.count { it.status == "READY" }
            val dbFailed = dbStickers.count { it.status == "FAILED" }
            Log.i(TAG, "Conversion:batchDBCheckpoint pack=$currentPackName total=${dbStickers.size} ready=$dbReady failed=$dbFailed")

            _progressData.value = _progressData.value.copy(
                isBatchFinished = true,
                isAllFinished = currentIndex >= allTelegramStickers.size
            )
        }
    }

    private fun applyInsertedIds(startIndex: Int, ids: LongArray) {
        if (ids.isEmpty()) return
        val currentList = _stickers.value.toMutableList()
        ids.forEachIndexed { offset, id ->
            val index = startIndex + offset
            if (index in currentList.indices) {
                currentList[index] = currentList[index].copy(id = id)
            }
        }
        _stickers.value = currentList
    }

    private fun getStickerId(index: Int): Long {
        return _stickers.value.getOrNull(index)?.id ?: 0L
    }

    private suspend fun setStatus(index: Int, stickerId: Long, status: String) {
        updateStickerInMemory(index, status, imagePath = null)
        if (stickerId > 0) {
            repository.updateStickerStatus(stickerId, status)
        }
    }

    private suspend fun markReady(index: Int, stickerId: Long, imagePath: String) {
        updateStickerInMemory(index, "READY", imagePath)
        if (stickerId > 0) {
            repository.updateStickerStatusAndFile(stickerId, "READY", imagePath)
        }
    }

    private suspend fun markFailed(index: Int, stickerId: Long) {
        updateStickerInMemory(index, "FAILED", imagePath = null)
        if (stickerId > 0) {
            repository.updateStickerStatus(stickerId, "FAILED")
        }
    }

    private fun updateStickerInMemory(index: Int, status: String, imagePath: String?) {
        val currentList = _stickers.value.toMutableList()
        val item = currentList.getOrNull(index) ?: return
        currentList[index] = item.copy(
            status = status,
            imageFile = imagePath ?: item.imageFile
        )
        _stickers.value = currentList
    }

    private suspend fun ensureTrayIconIfMissing(packDir: File, firstStickerFile: File) {
        val pack = repository.getPackById(currentPackName)
        if (pack != null && pack.trayImageFile.isEmpty()) {
            val trayFile = File(packDir, "tray.webp")
            ImageProcessor.processTrayIcon(firstStickerFile, trayFile)
            repository.updatePack(pack.copy(trayImageFile = trayFile.absolutePath))
        }
    }

    private fun updateProgress(batchDone: Int, batchTotal: Int, overallDone: Int, overallTotal: Int) {
        val readyCount = _stickers.value.count { it.status == "READY" }
        val elapsedSeconds = ((System.currentTimeMillis() - batchStartTimeMillis) / 1000.0).coerceAtLeast(0.001)
        val speed = batchDone / elapsedSeconds
        val etaSeconds = if (speed > 0.0) {
            ((batchTotal - batchDone) / speed).toLong().coerceAtLeast(0)
        } else {
            null
        }

        _progressData.value = _progressData.value.copy(
            overallCompleted = overallDone,
            overallTotal = overallTotal,
            batchCompleted = batchDone,
            batchTotal = batchTotal,
            readyCount = readyCount,
            speedStickersPerSec = speed,
            etaSeconds = etaSeconds
        )
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
