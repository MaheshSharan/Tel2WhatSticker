package com.maheshsharan.tel2what.ui.trayicon

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.maheshsharan.tel2what.utils.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class TrayIconSelectionFragment : Fragment(R.layout.fragment_tray_icon_selection) {

    private lateinit var viewModel: TrayIconViewModel
    private lateinit var adapter: TrayIconAdapter

    private lateinit var repository: StickerRepository

    private val pickCustomImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val packName = arguments?.getString("packName") ?: return@registerForActivityResult
        if (uri != null) {
            saveCustomTrayIcon(packName, uri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        repository = StickerRepository(database.stickerDao(), TelegramBotApi(), FileDownloader())
        val factory = TrayIconViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TrayIconViewModel::class.java]

        val btnBack: ImageView = view.findViewById(R.id.btnBack)
        val btnContinue: Button = view.findViewById(R.id.btnContinue)
        val btnSkip: Button = view.findViewById(R.id.btnSkip)
        val imgTrayPreview: ImageView = view.findViewById(R.id.imgTrayPreview)
        val recyclerTrayOptions: RecyclerView = view.findViewById(R.id.recyclerTrayOptions)
        val btnCustomImage: Button = view.findViewById(R.id.btnCustomImage)

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

        btnCustomImage.setOnClickListener {
            pickCustomImage.launch("image/*")
        }

        btnContinue.setOnClickListener {
            btnContinue.isEnabled = false
            viewModel.saveTrayIconAndContinue()
        }
    }

    private fun saveCustomTrayIcon(packName: String, uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val tempFile = File(requireContext().cacheDir, "tray_${UUID.randomUUID()}")
                requireContext().contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val packDir = File(requireContext().filesDir, "packs/$packName")
                packDir.mkdirs()
                val finalTrayFile = File(packDir, "tray.webp")

                val success = ImageProcessor.processTrayIcon(tempFile, finalTrayFile)
                tempFile.delete()

                if (!success) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Failed to process tray icon", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val pack = repository.getPackById(packName)
                if (pack != null) {
                    repository.updatePack(pack.copy(trayImageFile = finalTrayFile.absolutePath))
                }

                withContext(Dispatchers.Main) {
                    viewModel.selectIcon(finalTrayFile.absolutePath)
                    Toast.makeText(requireContext(), "Custom tray icon applied", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to set custom tray icon", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
