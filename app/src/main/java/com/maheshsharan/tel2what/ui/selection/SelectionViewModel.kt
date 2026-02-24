package com.maheshsharan.tel2what.ui.selection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maheshsharan.tel2what.data.local.entity.StickerEntity
import com.maheshsharan.tel2what.data.repository.StickerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SelectableSticker(
    val entity: StickerEntity,
    var isSelected: Boolean = false
)

class SelectionViewModel(private val repository: StickerRepository) : ViewModel() {

    private val _stickers = MutableStateFlow<List<SelectableSticker>>(emptyList())
    val stickers: StateFlow<List<SelectableSticker>> = _stickers.asStateFlow()

    private val _selectedCount = MutableStateFlow(0)
    val selectedCount: StateFlow<Int> = _selectedCount.asStateFlow()

    fun loadStickers(packId: String) {
        viewModelScope.launch {
            val dbStickers = repository.getStickersForPackSync(packId)
            // Only load successfully converted stickers
            val readyStickers = dbStickers.filter { it.status == "READY" }
            
            // Auto-select up to 30
            val initialList = readyStickers.mapIndexed { index, entity ->
                SelectableSticker(entity, isSelected = index < 30)
            }
            
            _stickers.value = initialList
            updateCount()
        }
    }

    fun toggleSelection(stickerId: Long) {
        val currentList = _stickers.value.toMutableList()
        val index = currentList.indexOfFirst { it.entity.id == stickerId }
        
        if (index != -1) {
            val item = currentList[index]
            
            // Prevent selecting more than 30
            if (!item.isSelected && _selectedCount.value >= 30) {
                return
            }
            
            currentList[index] = item.copy(isSelected = !item.isSelected)
            _stickers.value = currentList
            updateCount()
        }
    }

    fun selectAllAvailable() {
        val currentList = _stickers.value.toMutableList()
        var newlySelected = 0
        
        for (i in currentList.indices) {
            val item = currentList[i]
            if (!item.isSelected && _selectedCount.value + newlySelected < 30) {
                currentList[i] = item.copy(isSelected = true)
                newlySelected++
            }
        }
        
        if (newlySelected > 0) {
            _stickers.value = currentList
            updateCount()
        }
    }

    private fun updateCount() {
        _selectedCount.value = _stickers.value.count { it.isSelected }
    }
    
    fun getSelectedStickerIds(): List<Long> {
        return _stickers.value.filter { it.isSelected }.map { it.entity.id }
    }
}

class SelectionViewModelFactory(private val repository: StickerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SelectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SelectionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
