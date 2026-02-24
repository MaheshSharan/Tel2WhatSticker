package com.maheshsharan.tel2what.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.maheshsharan.tel2what.data.repository.StickerRepository
import kotlinx.coroutines.flow.map

class HomeViewModel(private val repository: StickerRepository) : ViewModel() {

    // Expose the list of packs from the database as a Flow to the UI
    val recentPacks = repository.getAllPacks().map { packs ->
        packs.sortedByDescending { it.dateAdded }
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
