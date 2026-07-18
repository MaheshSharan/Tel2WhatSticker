package com.maheshsharan.tel2what.ui.selection

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maheshsharan.tel2what.data.local.entity.StickerEntity
import com.maheshsharan.tel2what.data.repository.StickerRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SelectionEvent {
    data class ShowMixedPackPrompt(
        val stickerId: Long,
        val targetIsAnimated: Boolean
    ) : SelectionEvent()
}

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

    private val _events = MutableSharedFlow<SelectionEvent>()
    val events: SharedFlow<SelectionEvent> = _events.asSharedFlow()

    fun loadStickers(packId: String) {
        Log.i(TAG, "SelectionVM:loadStickers packId=$packId")
        viewModelScope.launch {
            val dbStickers = repository.getStickersForPackSync(packId)
            val readyStickers = dbStickers.filter { it.status == "READY" }
            Log.i(TAG, "SelectionVM:db total=${dbStickers.size} ready=${readyStickers.size}")

            val animatedStickers = readyStickers.filter { it.isAnimated }
            val staticStickers = readyStickers.filter { !it.isAnimated }

            // Priority: select only animated ones first
            val initialSelectedIds = if (animatedStickers.isNotEmpty()) {
                animatedStickers.take(30).map { it.id }
            } else {
                staticStickers.take(30).map { it.id }
            }

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

            if (item.isSelected) {
                // Deselecting is always safe
                currentList[index] = item.copy(isSelected = false)
                _stickers.value = currentList
                updateCount()
                viewModelScope.launch {
                    if (stickerId > 0) {
                        repository.setStickerSelected(stickerId, false)
                    }
                }
            } else {
                // Selecting: enforce no-mixing of animated and static stickers
                if (_selectedCount.value >= 30) {
                    return
                }

                val hasAnimatedSelected = currentList.any { it.isSelected && it.entity.isAnimated }
                val hasStaticSelected = currentList.any { it.isSelected && !it.entity.isAnimated }

                if (item.entity.isAnimated && hasStaticSelected) {
                    viewModelScope.launch {
                        _events.emit(SelectionEvent.ShowMixedPackPrompt(stickerId, true))
                    }
                } else if (!item.entity.isAnimated && hasAnimatedSelected) {
                    viewModelScope.launch {
                        _events.emit(SelectionEvent.ShowMixedPackPrompt(stickerId, false))
                    }
                } else {
                    // Safe to select
                    currentList[index] = item.copy(isSelected = true)
                    _stickers.value = currentList
                    updateCount()
                    viewModelScope.launch {
                        if (stickerId > 0) {
                            repository.setStickerSelected(stickerId, true)
                        }
                    }
                }
            }
        }
    }

    fun forceToggleSelection(stickerId: Long, isAnimated: Boolean) {
        viewModelScope.launch {
            val currentList = _stickers.value.toMutableList()

            // Deselect all of the other type
            val idsToDeselect = mutableListOf<Long>()
            for (i in currentList.indices) {
                val item = currentList[i]
                if (item.isSelected && item.entity.isAnimated != isAnimated) {
                    currentList[i] = item.copy(isSelected = false)
                    idsToDeselect.add(item.entity.id)
                }
            }

            // Select the target sticker
            val targetIndex = currentList.indexOfFirst { it.entity.id == stickerId }
            if (targetIndex != -1) {
                val item = currentList[targetIndex]
                currentList[targetIndex] = item.copy(isSelected = true)
            }

            _stickers.value = currentList
            updateCount()

            // Update DB
            idsToDeselect.filter { it > 0 }.forEach { id ->
                repository.setStickerSelected(id, false)
            }
            if (stickerId > 0) {
                repository.setStickerSelected(stickerId, true)
            }
        }
    }

    fun toggleSelectAllOrNone() {
        viewModelScope.launch {
            val currentList = _stickers.value.toMutableList()
            val packId = currentList.firstOrNull()?.entity?.packId ?: return@launch

            if (_selectedCount.value > 0) {
                // Deselect all
                for (i in currentList.indices) {
                    currentList[i] = currentList[i].copy(isSelected = false)
                }
                _stickers.value = currentList
                updateCount()
                repository.clearSelection(packId)
            } else {
                // Select all (up to 30, prioritizing animated)
                val animatedStickers = currentList.filter { it.entity.isAnimated }
                val staticStickers = currentList.filter { !it.entity.isAnimated }

                val targets = if (animatedStickers.isNotEmpty()) {
                    animatedStickers.take(30)
                } else {
                    staticStickers.take(30)
                }

                val targetIds = targets.map { it.entity.id }
                for (i in currentList.indices) {
                    val item = currentList[i]
                    if (targetIds.contains(item.entity.id)) {
                        currentList[i] = item.copy(isSelected = true)
                    }
                }

                _stickers.value = currentList
                updateCount()

                repository.clearSelection(packId)
                targetIds.filter { it > 0 }.forEach { id ->
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
