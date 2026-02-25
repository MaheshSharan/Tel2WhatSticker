package com.maheshsharan.tel2what.ui.selection

import android.util.Log
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

    private companion object {
        private const val TAG = "Tel2What"
    }

    private val _stickers = MutableStateFlow<List<SelectableSticker>>(emptyList())
    val stickers: StateFlow<List<SelectableSticker>> = _stickers.asStateFlow()

    private val _selectedCount = MutableStateFlow(0)
    val selectedCount: StateFlow<Int> = _selectedCount.asStateFlow()

    fun loadStickers(packId: String) {
        Log.i(TAG, "SelectionVM:loadStickers packId=$packId")
        viewModelScope.launch {
            val dbStickers = repository.getStickersForPackSync(packId)
            val readyStickers = dbStickers.filter { it.status == "READY" }
            Log.i(TAG, "SelectionVM:db total=${dbStickers.size} ready=${readyStickers.size}")

            val initialSelectedIds = readyStickers.take(30).map { it.id }

            repository.clearSelection(packId)
            initialSelectedIds.forEach { id ->
                if (id > 0) {
                    repository.setStickerSelected(id, true)
                }
            }

            val initialList = readyStickers.map { entity ->
                SelectableSticker(entity, isSelected = initialSelectedIds.contains(entity.id))
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

            if (!item.isSelected && _selectedCount.value >= 30) {
                return
            }

            val newSelected = !item.isSelected
            currentList[index] = item.copy(isSelected = newSelected)
            _stickers.value = currentList
            updateCount()

            viewModelScope.launch {
                if (stickerId > 0) {
                    repository.setStickerSelected(stickerId, newSelected)
                }
            }
        }
    }

    fun selectAllAvailable() {
        val currentList = _stickers.value.toMutableList()
        val newlySelectedIds = mutableListOf<Long>()

        for (i in currentList.indices) {
            val item = currentList[i]
            if (!item.isSelected && _selectedCount.value + newlySelectedIds.size < 30) {
                currentList[i] = item.copy(isSelected = true)
                newlySelectedIds.add(item.entity.id)
            }
        }

        if (newlySelectedIds.isNotEmpty()) {
            _stickers.value = currentList
            updateCount()

            viewModelScope.launch {
                newlySelectedIds.filter { it > 0 }.forEach { id ->
                    repository.setStickerSelected(id, true)
                }
            }
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
