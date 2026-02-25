package com.maheshsharan.tel2what.ui.importpack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maheshsharan.tel2what.data.network.model.TelegramStickerSet
import com.maheshsharan.tel2what.data.repository.StickerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    data class Success(val stickerSet: TelegramStickerSet) : ImportState()
    data class AlreadyDownloaded(
        val packId: String,
        val packTitle: String,
        val stickerCount: Int
    ) : ImportState()
    data class Error(val message: String) : ImportState()
}

class TelegramImportViewModel(private val repository: StickerRepository) : ViewModel() {

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    fun fetchPackMetadata(url: String) {
        val packName = extractPackName(url)
        if (packName == null) {
            _importState.value = ImportState.Error("Invalid Telegram Sticker Link. Must be https://t.me/addstickers/...")
            return
        }

        _importState.value = ImportState.Loading

        viewModelScope.launch {
            // CACHE CHECK: Query the DB to prevent re-downloads
            val existingPack = repository.getPackById(packName)
            if (existingPack != null) {
                val packSize = repository.getStickersForPackSync(packName).size
                _importState.value = ImportState.AlreadyDownloaded(
                    packId = packName,
                    packTitle = existingPack.name,
                    stickerCount = packSize
                )
                return@launch
            }

            val result = repository.fetchTelegramPackMetadata(packName)
            if (result.isSuccess) {
                val stickerSet = result.getOrNull()
                // Ignore empty packs
                if (stickerSet == null || stickerSet.stickers.isEmpty()) {
                    _importState.value = ImportState.Error("This sticker pack is empty. It has 0 stickers.")
                } else {
                    _importState.value = ImportState.Success(stickerSet)
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to fetch pack. Unknown error."
                _importState.value = ImportState.Error(errorMsg)
            }
        }
    }

    private fun extractPackName(url: String): String? {
        val trimmedUrl = url.trim()
        val prefix1 = "https://t.me/addstickers/"
        val prefix2 = "t.me/addstickers/"
        
        var packName = ""
        
        if (trimmedUrl.startsWith(prefix1)) {
            packName = trimmedUrl.removePrefix(prefix1)
        } else if (trimmedUrl.startsWith(prefix2)) {
            packName = trimmedUrl.removePrefix(prefix2)
        } else {
            // Also allow them to just paste the raw pack name (e.g. "AnimalPack") 
            // if it doesn't contain slashes or spaces.
            if (!trimmedUrl.contains("/") && !trimmedUrl.contains(" ")) {
                packName = trimmedUrl
            } else {
                return null
            }
        }
        
        // Remove trailing slash if accidentally added
        packName = packName.removeSuffix("/")
        
        return packName
    }
}

class TelegramImportViewModelFactory(private val repository: StickerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TelegramImportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TelegramImportViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
