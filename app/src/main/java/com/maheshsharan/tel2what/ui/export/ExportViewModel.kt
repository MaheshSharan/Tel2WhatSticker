package com.maheshsharan.tel2what.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maheshsharan.tel2what.data.local.entity.StickerPackEntity
import com.maheshsharan.tel2what.data.repository.StickerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExportViewModel(private val repository: StickerRepository) : ViewModel() {

    private val _pack = MutableStateFlow<StickerPackEntity?>(null)
    val pack: StateFlow<StickerPackEntity?> = _pack.asStateFlow()

    private val _stickersCount = MutableStateFlow(0)
    val stickersCount: StateFlow<Int> = _stickersCount.asStateFlow()

    fun loadPackDetails(packName: String) {
        viewModelScope.launch {
            val packEntity = repository.getPackById(packName)
            _pack.value = packEntity
            
            val stickers = repository.getSelectedReadyStickersForPackSync(packName)
            _stickersCount.value = stickers.size
        }
    }

    suspend fun updatePackDetailsAndSave(name: String, author: String): Boolean {
        val currentPack = _pack.value ?: return false
        val newName = name.ifEmpty { currentPack.name }
        val newAuthor = author.ifEmpty { "Telegram User" }
        
        val updatedPack = currentPack.copy(
            name = newName,
            publisher = newAuthor
        )
        
        repository.updatePack(updatedPack)
        _pack.value = updatedPack
        return true
    }
}

class ExportViewModelFactory(private val repository: StickerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExportViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
