package com.maheshsharan.tel2what.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.maheshsharan.tel2what.data.local.entity.StickerPackEntity
import com.maheshsharan.tel2what.data.repository.StickerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val packs: List<StickerPackEntity>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(private val repository: StickerRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    // Expose the list of packs from the database as a Flow to the UI
    val recentPacks = repository.getAllPacks()
        .map { packs ->
            val sorted = packs.sortedByDescending { it.dateAdded }
            _uiState.value = HomeUiState.Success(sorted)
            sorted
        }
        .catch { e ->
            Log.e("Tel2What:Home", "Error loading packs", e)
            _uiState.value = HomeUiState.Error(e.message ?: "Failed to load sticker packs")
            emit(emptyList())
        }
}

class HomeViewModelFactory(private val repository: StickerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
