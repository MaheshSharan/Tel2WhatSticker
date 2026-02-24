package com.maheshsharan.tel2what.ui.home

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.maheshsharan.tel2what.R
import com.maheshsharan.tel2what.data.local.AppDatabase
import com.maheshsharan.tel2what.data.network.FileDownloader
import com.maheshsharan.tel2what.data.network.TelegramBotApi
import com.maheshsharan.tel2what.data.repository.StickerRepository
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: RecentPacksAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup ViewModel
        val database = AppDatabase.getDatabase(requireContext())
        val repository = StickerRepository(database.stickerDao(), TelegramBotApi(), FileDownloader())
        val factory = HomeViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]

        val cardImportTelegram: MaterialCardView = view.findViewById(R.id.cardImportTelegram)
        val cardManualUpload: MaterialCardView = view.findViewById(R.id.cardManualUpload)
        val btnStorage: ImageView = view.findViewById(R.id.btnStorage)
        val recyclerRecentPacks: RecyclerView = view.findViewById(R.id.recyclerRecentPacks)

        // Setup RecyclerView
        adapter = RecentPacksAdapter { _ ->
            // Handle clicking a recent pack (could navigate to details or intent to WA)
        }
        recyclerRecentPacks.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerRecentPacks.adapter = adapter

        // Observe Data
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recentPacks.collect { packs ->
                adapter.submitList(packs)
            }
        }

        cardImportTelegram.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_telegramImportFragment)
        }

        cardManualUpload.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_manualUploadFragment)
        }

        btnStorage.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_storageManagementFragment)
        }
    }
}
