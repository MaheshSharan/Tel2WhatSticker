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
        val txtEtaSpeed: TextView = view.findViewById(R.id.txtEtaSpeed)
        val txtPercent: TextView = view.findViewById(R.id.txtPercent)
        val txtOverallProgress: TextView = view.findViewById(R.id.txtOverallProgress)
        val toolbar: Toolbar = view.findViewById(R.id.toolbar)

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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.progressData.collect { progress ->
                val overallTotal = progress.overallTotal.coerceAtLeast(1)
                val overallCompleted = progress.overallCompleted.coerceIn(0, overallTotal)

                progressBar.max = overallTotal
                progressBar.progress = overallCompleted

                val percent = (overallCompleted * 100) / overallTotal
                txtPercent.text = "$percent%"

                val etaStr = formatEta(progress.etaSeconds)
                val speedStr = formatSpeed(progress.speedStickersPerSec)
                txtEtaSpeed.text = "$speedStr • ETA $etaStr"

                txtOverallProgress.text = "$overallCompleted / $overallTotal"

                btnDownloadMore.isEnabled = progress.isBatchFinished && !progress.isAllFinished && !progress.isError
                btnContinue.isEnabled = progress.readyCount > 0 && !progress.isError
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

    private fun formatEta(seconds: Long?): String {
        if (seconds == null) return "--:--"
        val s = seconds.coerceAtLeast(0)
        val m = s / 60
        val r = s % 60
        return String.format("%02d:%02d", m, r)
    }

    private fun formatSpeed(speed: Double): String {
        if (!speed.isFinite() || speed <= 0.0) return "Speed --"
        return String.format("Speed %.1f st/s", speed)
    }
}
