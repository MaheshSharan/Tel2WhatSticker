package com.maheshsharan.tel2what.ui.manual

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.maheshsharan.tel2what.R
import com.maheshsharan.tel2what.data.local.AppDatabase
import com.maheshsharan.tel2what.data.network.FileDownloader
import com.maheshsharan.tel2what.data.network.TelegramBotApi
import com.maheshsharan.tel2what.data.repository.StickerRepository
import kotlinx.coroutines.launch

class ManualUploadFragment : Fragment(R.layout.fragment_manual_upload) {

    private lateinit var viewModel: ManualUploadViewModel
    private lateinit var adapter: ManualUploadAdapter

    private val selectImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addFiles(uris)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        val repository = StickerRepository(database.stickerDao(), TelegramBotApi(), FileDownloader())
        val factory = ManualUploadViewModelFactory(repository, requireContext())
        viewModel = ViewModelProvider(this, factory)[ManualUploadViewModel::class.java]

        val btnBack: ImageView = view.findViewById(R.id.btnBack)
        val btnProcess: Button = view.findViewById(R.id.btnProcess)
        val btnClear: TextView = view.findViewById(R.id.btnClear)
        val cardAddFiles: MaterialCardView = view.findViewById(R.id.cardAddFiles)
        val txtSelectedCount: TextView = view.findViewById(R.id.txtSelectedCount)
        val recyclerManualUploads: RecyclerView = view.findViewById(R.id.recyclerManualUploads)

        adapter = ManualUploadAdapter { uri ->
            viewModel.removeFile(uri)
        }
        recyclerManualUploads.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedFiles.collect { items ->
                adapter.submitList(items, viewModel.isProcessing.value)
                txtSelectedCount.text = "${items.size}/30"
                btnProcess.isEnabled = items.size in 3..30 && !viewModel.isProcessing.value
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isProcessing.collect { isProcessing ->
                adapter.submitList(viewModel.selectedFiles.value, isProcessing)
                cardAddFiles.isEnabled = !isProcessing
                btnClear.isEnabled = !isProcessing
                if (isProcessing) {
                    btnProcess.text = "Processing..."
                    btnProcess.isEnabled = false
                } else {
                    btnProcess.text = "Process Files"
                    val count = viewModel.selectedFiles.value.size
                    btnProcess.isEnabled = count in 3..30
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.processSuccess.collect { packId ->
                if (packId != null) {
                    // Navigate to Export directly since tray icon is auto-generated from first sticker
                    val bundle = Bundle().apply {
                        putString("packName", packId)
                    }
                    findNavController().navigate(R.id.action_manualUploadFragment_to_stickerSelectionFragment, bundle)
                }
            }
        }

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        btnClear.setOnClickListener {
            viewModel.clearFiles()
        }

        cardAddFiles.setOnClickListener {
            if (viewModel.selectedFiles.value.size < 30) {
                selectImagesLauncher.launch("image/*")
            } else {
                Toast.makeText(requireContext(), "You can only select up to 30 files.", Toast.LENGTH_SHORT).show()
            }
        }

        btnProcess.setOnClickListener {
            viewModel.processFiles()
        }
    }
}
