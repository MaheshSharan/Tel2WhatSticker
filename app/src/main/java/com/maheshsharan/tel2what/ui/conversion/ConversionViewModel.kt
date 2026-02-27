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
import kotlinx.coroutines.cancelChildren
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
    val errorMessage: String = "",
    val isStopped: Boolean = false
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
    private var currentPackIsAnimated = false
    
    private val conversionEngine = com.maheshsharan.tel2what.engine.StickerConversionEngine(context)

    private var overallStartTimeMillis: Long = 0L
    private var batchStartTimeMillis: Long = 0L
    private var isStopped = false

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
                    // A pack is animated if it's explicitly marked OR contains any animated/video stickers
                    currentPackIsAnimated = set.stickerType != "regular" || allTelegramStickers.any { it.isAnimated || it.isVideo }
                    
                    Log.i(TAG, "Conversion:metadata ok pack=$packName total=${allTelegramStickers.size} type=${set.stickerType} isAnimated=$currentPackIsAnimated")
                    Log.i(TAG, "Conversion:stickerBreakdown animated=${allTelegramStickers.count { it.isAnimated }} video=${allTelegramStickers.count { it.isVideo }} static=${allTelegramStickers.count { !it.isAnimated && !it.isVideo }}")
                    
                    // Log first few stickers for debugging
                    allTelegramStickers.take(3).forEachIndexed { idx, sticker ->
                        Log.i(TAG, "Conversion:stickerSample[$idx] fileId=${sticker.fileId} isAnimated=${sticker.isAnimated} isVideo=${sticker.isVideo} emoji=${sticker.emoji}")
                    }

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
                            animatedStickerPack = currentPackIsAnimated,
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
        if (isStopped) {
            Log.i(TAG, "Conversion:downloadNextBatch - Stopped, ignoring request")
            return
        }
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
                    if (isStopped) {
                        Log.i(TAG, "Conversion:stickerSkipped idx=$index reason=stopped")
                        return@async
                    }
                    concurrencySemaphore.acquire()
                    val globalIndex = startIndexForThisBatch + index
                    val stickerId = getStickerId(globalIndex)

                    Log.i(TAG, "Conversion:stickerStart idx=$index globalIndex=$globalIndex id=$stickerId")

                    try {
                        Log.i(TAG, "Conversion:stickerStart idx=$index globalIndex=$globalIndex id=$stickerId fileId=${telegramSticker.fileId}")
                        Log.i(TAG, "Conversion:stickerFlags idx=$index isAnimated=${telegramSticker.isAnimated} isVideo=${telegramSticker.isVideo}")
                        
                        val filePathInfo = repository.fetchFilePath(telegramSticker.fileId)
                        if (filePathInfo == null) {
                            Log.e(TAG, "Conversion:stickerFail idx=$index id=$stickerId reason=filePathNull fileId=${telegramSticker.fileId}")
                            markFailed(globalIndex, stickerId)
                            return@async
                        }

                        Log.i(TAG, "Conversion:filePathInfo idx=$index filePathInfo='$filePathInfo'")
                        
                        val downloadUrl = repository.getDownloadUrl(filePathInfo)
                        Log.i(TAG, "Conversion:downloadUrl idx=$index url='$downloadUrl'")
                        
                        // Telegram file paths often look like stickers/file_0.tgs or documents/file_1.webm
                        // We MUST preserve the extension so the ConversionEngine can route to the correct Libwebp/Lottie decoder.
                        val extension = if (filePathInfo.contains(".")) {
                            "." + filePathInfo.substringAfterLast(".")
                        } else {
                            ""
                        }
                        
                        Log.i(TAG, "Conversion:extension idx=$index extension='$extension' hasExtension=${extension.isNotEmpty()}")
                        
                        val cacheFile = File(cacheDir, "${telegramSticker.fileUniqueId}$extension")
                        Log.i(TAG, "Conversion:cacheFile idx=$index path='${cacheFile.absolutePath}'")
                        
                        val downloadSuccess = repository.downloadBinary(downloadUrl, cacheFile)
                        Log.i(TAG, "Conversion:downloadResult idx=$index success=$downloadSuccess fileExists=${cacheFile.exists()} fileSize=${if (cacheFile.exists()) cacheFile.length() else 0}")

                        if (!downloadSuccess) {
                            Log.e(TAG, "Conversion:stickerFail idx=$index id=$stickerId reason=downloadFailed url=$downloadUrl")
                            markFailed(globalIndex, stickerId)
                            return@async
                        }

                        setStatus(globalIndex, stickerId, "CONVERTING")
                        Log.i(TAG, "Conversion:startConversion idx=$index id=$stickerId inputFile='${cacheFile.name}' isAnimatedPack=$currentPackIsAnimated")

                        val finalFile = File(finalDir, "${telegramSticker.fileUniqueId}.webp")
                        
                        // Hand off to the Media Transformation Engine
                        val result = conversionEngine.convertSticker(
                            inputFile = cacheFile,
                            outputFile = finalFile,
                            isAnimatedPack = currentPackIsAnimated
                        )
                        
                        Log.i(TAG, "Conversion:conversionResult idx=$index id=$stickerId resultType=${result::class.simpleName}")

                        if (result is com.maheshsharan.tel2what.engine.StickerConversionResult.Success) {
                            ensureTrayIconIfMissing(finalDir, finalFile)
                            Log.i(TAG, "Conversion:stickerReady idx=$index id=$stickerId file=${finalFile.name} size=${finalFile.length()} isAnimated=${result.isAnimated}")
                            markReady(globalIndex, stickerId, finalFile.absolutePath)
                        } else {
                            when (result) {
                                is com.maheshsharan.tel2what.engine.StickerConversionResult.Failed -> {
                                    val exceptionMsg = result.exception?.message ?: "no exception"
                                    Log.e(TAG, "Conversion:stickerFail idx=$index id=$stickerId type=Failed reason='${result.reason}' exception='$exceptionMsg'")
                                    if (result.exception != null) {
                                        Log.e(TAG, "Conversion:stickerFailStackTrace idx=$index", result.exception)
                                    }
                                }
                                is com.maheshsharan.tel2what.engine.StickerConversionResult.ValidationFailed -> {
                                    Log.e(TAG, "Conversion:stickerFail idx=$index id=$stickerId type=ValidationFailed reason='${result.reason}'")
                                }
                                else -> {
                                    Log.e(TAG, "Conversion:stickerFail idx=$index id=$stickerId type=Unknown")
                                }
                            }
                            markFailed(globalIndex, stickerId)
                        }

                        if (cacheFile.exists()) {
                            cacheFile.delete()
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Conversion:stickerException idx=$index id=$stickerId exception=${e.message}", e)
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

    fun stopConversion() {
        Log.i(TAG, "Conversion:stopConversion - User requested stop")
        isStopped = true
        
        // Mark all DOWNLOADING and CONVERTING stickers as STOPPED
        val currentList = _stickers.value.toMutableList()
        currentList.forEachIndexed { index, sticker ->
            if (sticker.status == "DOWNLOADING" || sticker.status == "CONVERTING") {
                currentList[index] = sticker.copy(status = "STOPPED")
                // Update in database too
                if (sticker.id > 0) {
                    viewModelScope.launch {
                        repository.updateStickerStatus(sticker.id, "STOPPED")
                    }
                }
            }
        }
        _stickers.value = currentList
        
        _progressData.value = _progressData.value.copy(
            isStopped = true,
            isBatchFinished = true
        )
        
        // Cancel all running coroutines
        viewModelScope.coroutineContext.cancelChildren()
    }

    override fun onCleared() {
        super.onCleared()
        Log.i(TAG, "Conversion:onCleared - Cancelling all conversion jobs")
        // Cancel all coroutines when ViewModel is destroyed (user navigates away)
        // viewModelScope automatically cancels all child coroutines
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
