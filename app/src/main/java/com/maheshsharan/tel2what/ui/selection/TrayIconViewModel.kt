package com.maheshsharan.tel2what.ui.selection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maheshsharan.tel2what.data.local.entity.StickerEntity
import com.maheshsharan.tel2what.data.repository.StickerRepository
import com.maheshsharan.tel2what.utils.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class TrayIconViewModel(private val repository: StickerRepository) : ViewModel() {

    private val _stickers = MutableStateFlow<List<StickerEntity>>(emptyList())
    val stickers: StateFlow<List<StickerEntity>> = _stickers.asStateFlow()

    private val _selectedIconPath = MutableStateFlow<String?>(null)
    val selectedIconPath: StateFlow<String?> = _selectedIconPath.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private var currentPackName = ""

    fun loadSelectedStickers(packName: String, selectedIds: LongArray) {
        currentPackName = packName
        viewModelScope.launch {
            val allStickers = repository.getStickersForPackSync(packName)
            val filtered = allStickers.filter { selectedIds.contains(it.id) }
            _stickers.value = filtered
            
            // Default to the first selected sticker if available
            if (filtered.isNotEmpty()) {
                selectIcon(filtered.first().imageFile)
            }
        }
    }

    fun selectIcon(path: String) {
        _selectedIconPath.value = path
    }

    fun saveTrayIconAndContinue() {
        val selectedPath = _selectedIconPath.value
        if (selectedPath.isNullOrEmpty()) {
            _isSaved.value = true // Nothing to do, skip
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val file = File(selectedPath)
            if (file.exists()) {
                val packDir = file.parentFile
                val finalTrayFile = File(packDir, "tray.webp")
                
                // If it's the exact same file, we shouldn't overwrite it directly during process,
                // but processTrayIcon handles creating a new file.
                ImageProcessor.processTrayIcon(file, finalTrayFile)

                val pack = repository.getPackById(currentPackName)
                if (pack != null) {
                    repository.insertPack(pack.copy(trayImageFile = finalTrayFile.absolutePath))
                }
            }
            
            withContext(Dispatchers.Main) {
                _isSaved.value = true
            }
        }
    }
}

class TrayIconViewModelFactory(private val repository: StickerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrayIconViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TrayIconViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
