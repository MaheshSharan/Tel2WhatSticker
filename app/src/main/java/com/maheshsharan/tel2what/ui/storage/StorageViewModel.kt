package com.maheshsharan.tel2what.ui.storage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maheshsharan.tel2what.data.local.entity.StickerPackEntity
import com.maheshsharan.tel2what.data.repository.StickerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat

data class PackStorageInfo(
    val pack: StickerPackEntity,
    val totalStickers: Int,
    val sizeBytes: Long
)

class StorageViewModel(
    private val repository: StickerRepository,
    private val context: Context
) : ViewModel() {

    private val _storageInfo = MutableStateFlow<List<PackStorageInfo>>(emptyList())
    val storageInfo: StateFlow<List<PackStorageInfo>> = _storageInfo.asStateFlow()

    private val _totalUsageBytes = MutableStateFlow(0L)
    val totalUsageBytes: StateFlow<Long> = _totalUsageBytes.asStateFlow()

    fun loadStorageData() {
        viewModelScope.launch {
            val packs = repository.getAllPacksSync()
            val packInfos = mutableListOf<PackStorageInfo>()
            var totalBytes = 0L

            for (pack in packs) {
                val packDir = File(pack.trayImageFile).parentFile
                val bytes = getFolderSize(packDir)
                val stickers = repository.getStickersForPackSync(pack.identifier)
                packInfos.add(PackStorageInfo(pack, stickers.size, bytes))
                totalBytes += bytes
            }

            _storageInfo.value = packInfos
            _totalUsageBytes.value = totalBytes
        }
    }

    private fun getFolderSize(folder: File?): Long {
        if (folder == null || !folder.exists()) return 0
        var length = 0L
        val files = folder.listFiles() ?: return 0
        for (file in files) {
            if (file.isFile) {
                length += file.length()
            } else {
                length += getFolderSize(file)
            }
        }
        return length
    }

    fun clearPackCache(packId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val packCacheDir = File(context.cacheDir, "telegram/$packId")
            packCacheDir.deleteRecursively()
            withContext(Dispatchers.Main) {
                loadStorageData()
            }
        }
    }

    fun deletePack(packId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val pack = repository.getPackById(packId)
            if (pack != null) {
                val packDir = File(pack.trayImageFile).parentFile
                packDir?.deleteRecursively()
                repository.deletePack(packId)
                repository.deleteStickersForPack(packId)
            }
            withContext(Dispatchers.Main) {
                loadStorageData()
            }
        }
    }

    fun deleteAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val packsDir = File(context.filesDir, "packs")
            packsDir.deleteRecursively()
            
            val packs = repository.getAllPacksSync()
            packs.forEach { pack ->
                repository.deletePack(pack.identifier)
                repository.deleteStickersForPack(pack.identifier)
            }
            withContext(Dispatchers.Main) {
                loadStorageData()
            }
        }
    }
}

class StorageViewModelFactory(
    private val repository: StickerRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StorageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StorageViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

fun formatFileSize(sizeBytes: Long): String {
    if (sizeBytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(sizeBytes.toDouble()) / Math.log10(1024.0)).toInt()
    val dec = DecimalFormat("#,##0.#")
    dec.roundingMode = RoundingMode.CEILING
    return dec.format(sizeBytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}
