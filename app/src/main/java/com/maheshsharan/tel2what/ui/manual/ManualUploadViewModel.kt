package com.maheshsharan.tel2what.ui.manual

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maheshsharan.tel2what.data.local.entity.StickerEntity
import com.maheshsharan.tel2what.data.local.entity.StickerPackEntity
import com.maheshsharan.tel2what.data.repository.StickerRepository
import com.maheshsharan.tel2what.engine.ConversionConfig
import com.maheshsharan.tel2what.engine.StickerConversionResult
import com.maheshsharan.tel2what.engine.StaticStickerConverter
import com.maheshsharan.tel2what.utils.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ManualUploadItem(
    val uri: Uri,
    var status: String = "IDLE", // IDLE, PROCESSING, READY, FAILED
    var finalPath: String = ""
)

class ManualUploadViewModel(
    private val repository: StickerRepository,
    private val context: Context
) : ViewModel() {

    private val staticConverter = StaticStickerConverter()
    private val config = ConversionConfig()

    private val _selectedFiles = MutableStateFlow<List<ManualUploadItem>>(emptyList())
    val selectedFiles: StateFlow<List<ManualUploadItem>> = _selectedFiles.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processSuccess = MutableStateFlow<String?>(null) // returns packId if success
    val processSuccess: StateFlow<String?> = _processSuccess.asStateFlow()

    fun addFiles(uris: List<Uri>) {
        val currentSize = _selectedFiles.value.size
        val itemsToAdd = uris.take(30 - currentSize).map { ManualUploadItem(it) }
        _selectedFiles.value = _selectedFiles.value + itemsToAdd
    }

    fun removeFile(uri: Uri) {
        _selectedFiles.value = _selectedFiles.value.filter { it.uri != uri }
    }

    fun clearFiles() {
        _selectedFiles.value = emptyList()
    }

    fun processFiles() {
        if (_selectedFiles.value.isEmpty()) return
        
        _isProcessing.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val packId = "custom_${System.currentTimeMillis()}"
            val packDir = File(context.filesDir, "packs/$packId")
            packDir.mkdirs()

            val deferreds = _selectedFiles.value.mapIndexed { index, item ->
                async {
                    updateStatus(item.uri, "PROCESSING")
                    
                    try {
                        // Copy URI to temp cache file
                        val tempFile = File(context.cacheDir, "temp_${UUID.randomUUID()}")
                        context.contentResolver.openInputStream(item.uri)?.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }

                        val finalFile = File(packDir, "custom_${index}.webp")
                        // Use StaticStickerConverter instead of ImageProcessor.processStaticSticker
                        val result = staticConverter.convert(tempFile, finalFile, config)
                        val success = result is StickerConversionResult.Success

                        if (success) {
                            if (index == 0) {
                                val trayFile = File(packDir, "tray.webp")
                                ImageProcessor.processTrayIcon(finalFile, trayFile)
                            }
                            updateStatus(item.uri, "READY", finalFile.absolutePath)
                        } else {
                            updateStatus(item.uri, "FAILED")
                        }
                        tempFile.delete()

                    } catch (e: Exception) {
                        updateStatus(item.uri, "FAILED")
                    }
                }
            }

            deferreds.awaitAll()

            val successItems = _selectedFiles.value.filter { it.status == "READY" }
            if (successItems.size >= 3) {
                // Save pack
                val trayPath = File(packDir, "tray.webp").absolutePath
                val packEntity = StickerPackEntity(
                    identifier = packId,
                    name = "Custom Pack",
                    publisher = "Me",
                    trayImageFile = trayPath,
                    publisherEmail = "",
                    publisherWebsite = "",
                    privacyPolicyWebsite = "",
                    licenseAgreementWebsite = "",
                    animatedStickerPack = false,
                    imageDataVersion = "1",
                    avoidCache = false,
                    sizeBytes = 0,
                    dateAdded = System.currentTimeMillis()
                )
                repository.insertPack(packEntity)

                // Save stickers
                val entities = successItems.map { item ->
                    StickerEntity(
                        packId = packId,
                        imageFile = item.finalPath,
                        emojis = "😀",
                        accessibilityText = "Sticker",
                        status = "READY"
                    )
                }
                repository.insertStickers(entities)

                withContext(Dispatchers.Main) {
                    _processSuccess.value = packId
                }
            } else {
                // Clean up if < 3 succeeded (not enough for WA)
                packDir.deleteRecursively()
                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                    // Would be better to emit a UI ERROR state here
                }
            }
        }
    }

    private fun updateStatus(uri: Uri, status: String, finalPath: String = "") {
        val current = _selectedFiles.value.toMutableList()
        val index = current.indexOfFirst { it.uri == uri }
        if (index != -1) {
            val item = current[index]
            current[index] = item.copy(status = status, finalPath = finalPath)
            _selectedFiles.value = current
        }
    }
}

class ManualUploadViewModelFactory(
    private val repository: StickerRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManualUploadViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ManualUploadViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
