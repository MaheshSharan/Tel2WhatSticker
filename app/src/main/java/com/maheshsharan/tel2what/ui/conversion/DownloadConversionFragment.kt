package com.maheshsharan.tel2what.ui.conversion

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.maheshsharan.tel2what.R
import com.maheshsharan.tel2what.data.local.AppDatabase
import com.maheshsharan.tel2what.data.network.FileDownloader
import com.maheshsharan.tel2what.data.network.TelegramBotApi
import com.maheshsharan.tel2what.data.repository.StickerRepository
import kotlinx.coroutines.launch

class DownloadConversionFragment : Fragment(R.layout.fragment_download_conversion) {

    private lateinit var viewModel: ConversionViewModel
    private lateinit var adapter: DownloadStickerAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Setup ViewModel
        val database = AppDatabase.getDatabase(requireContext())
        val repository = StickerRepository(database.stickerDao(), TelegramBotApi(), FileDownloader())
        val factory = ConversionViewModelFactory(repository, requireContext())
        viewModel = ViewModelProvider(this, factory)[ConversionViewModel::class.java]

        val btnBack: ImageView = view.findViewById(R.id.btnBack)
        val btnContinue: Button = view.findViewById(R.id.btnContinue)
        val btnDownloadMore: Button = view.findViewById(R.id.btnDownloadMore)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val recyclerStickers: RecyclerView = view.findViewById(R.id.recyclerStickers)
        val toolbar: Toolbar = view.findViewById(R.id.toolbar)

        // Find progress text views manually based on layout structure or by traversing
        // For simplicity we will assume we can find them if they had IDs. 
        // Layout has nested linear layouts without IDs for text views.
        // For a robust implementation, standard practice is to add IDs to the TextViews in XML. 
        // We will skip updating specific text strings here unless IDs are present, and only update ProgressBar.

        // Setup RecyclerView
        adapter = DownloadStickerAdapter()
        recyclerStickers.adapter = adapter

        // Get Bundle Args passed from TelegramImportFragment
        val packName = arguments?.getString("packName") ?: "Unknown"
        val packTitle = arguments?.getString("packTitle") ?: "Sticker Pack"
        
        // Set Toolbar Title
        val txtPackTitle = view.findViewById<TextView>(R.id.txtPackTitle)
        txtPackTitle?.text = packTitle
        
        // Start Conversion Process
        viewModel.initAndStart(packName, packTitle)

        // Observe State
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.progressData.collect { progress ->
                progressBar.max = progress.totalToDownload
                progressBar.progress = progress.downloaded
                
                if (progress.isFinished) {
                    btnDownloadMore.isEnabled = true
                    btnContinue.isEnabled = true
                } else {
                    btnDownloadMore.isEnabled = false
                    btnContinue.isEnabled = false
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.stickers.collect { list ->
                adapter.submitList(list)
            }
        }

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        btnDownloadMore.setOnClickListener {
            viewModel.downloadNextBatch()
        }

        btnContinue.setOnClickListener {
            val bundle = Bundle().apply {
                putString("packName", packName)
            }
            findNavController().navigate(R.id.action_downloadConversionFragment_to_stickerSelectionFragment, bundle)
        }
    }
}
