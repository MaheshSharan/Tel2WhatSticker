package com.maheshsharan.tel2what.ui.trayicon

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.maheshsharan.tel2what.R
import com.maheshsharan.tel2what.data.local.AppDatabase
import com.maheshsharan.tel2what.data.network.FileDownloader
import com.maheshsharan.tel2what.data.network.TelegramBotApi
import com.maheshsharan.tel2what.data.repository.StickerRepository
import com.maheshsharan.tel2what.ui.selection.TrayIconAdapter
import com.maheshsharan.tel2what.ui.selection.TrayIconViewModel
import com.maheshsharan.tel2what.ui.selection.TrayIconViewModelFactory
import kotlinx.coroutines.launch
import java.io.File

class TrayIconSelectionFragment : Fragment(R.layout.fragment_tray_icon_selection) {

    private lateinit var viewModel: TrayIconViewModel
    private lateinit var adapter: TrayIconAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        val repository = StickerRepository(database.stickerDao(), TelegramBotApi(), FileDownloader())
        val factory = TrayIconViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TrayIconViewModel::class.java]

        val btnBack: ImageView = view.findViewById(R.id.btnBack)
        val btnContinue: Button = view.findViewById(R.id.btnContinue)
        val btnSkip: Button = view.findViewById(R.id.btnSkip)
        val imgTrayPreview: ImageView = view.findViewById(R.id.imgTrayPreview)
        val recyclerTrayOptions: RecyclerView = view.findViewById(R.id.recyclerTrayOptions)

        val packName = arguments?.getString("packName") ?: return
        val selectedIds = arguments?.getLongArray("selectedIds") ?: return

        adapter = TrayIconAdapter { imagePath ->
            viewModel.selectIcon(imagePath)
        }
        recyclerTrayOptions.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerTrayOptions.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.stickers.collect { items ->
                adapter.submitList(items, viewModel.selectedIconPath.value)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedIconPath.collect { path ->
                if (!path.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(File(path))
                        .into(imgTrayPreview)
                }
                adapter.submitList(viewModel.stickers.value, path)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isSaved.collect { saved ->
                if (saved) {
                    val bundle = Bundle().apply {
                        putString("packName", packName)
                    }
                    findNavController().navigate(R.id.action_trayIconSelectionFragment_to_exportFragment, bundle)
                }
            }
        }

        viewModel.loadSelectedStickers(packName, selectedIds)

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        btnSkip.setOnClickListener {
            val bundle = Bundle().apply {
                putString("packName", packName)
            }
            findNavController().navigate(R.id.action_trayIconSelectionFragment_to_exportFragment, bundle)
        }

        btnContinue.setOnClickListener {
            btnContinue.isEnabled = false
            viewModel.saveTrayIconAndContinue()
        }
    }
}
