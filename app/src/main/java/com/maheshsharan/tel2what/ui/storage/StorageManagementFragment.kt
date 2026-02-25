package com.maheshsharan.tel2what.ui.storage

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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

class StorageManagementFragment : Fragment(R.layout.fragment_storage_management) {

    private lateinit var viewModel: StorageViewModel
    private lateinit var adapter: StorageAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        val repository = StickerRepository(database.stickerDao(), TelegramBotApi(), FileDownloader())
        val factory = StorageViewModelFactory(repository, requireContext())
        viewModel = ViewModelProvider(this, factory)[StorageViewModel::class.java]

        val btnBack: ImageView = view.findViewById(R.id.btnBack)
        
        val btnClearCache: Button? = view.findViewById(R.id.btnClearCache)
        val btnDeleteAll: Button? = view.findViewById(R.id.btnDeleteAll)
        val recyclerStoragePacks: RecyclerView? = view.findViewById(R.id.recyclerStoragePacks)
        val txtTotalSize: TextView? = view.findViewById(R.id.txtTotalSize)

        adapter = StorageAdapter(
            onClearCacheClick = { packId, packName ->
                showConfirmationDialog(
                    title = "Clear Cache",
                    message = "Clear temporary cache for '$packName'?"
                ) {
                    viewModel.clearPackCache(packId)
                }
            },
            onDeleteClick = { packId, packName ->
                showConfirmationDialog(
                    title = "Confirm Deletion",
                    message = "Delete '$packName'? This removes the converted pack from storage."
                ) {
                    viewModel.deletePack(packId)
                }
            }
        )
        recyclerStoragePacks?.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.storageInfo.collect { infos ->
                adapter.submitList(infos)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalUsageBytes.collect { bytes ->
                txtTotalSize?.text = formatFileSize(bytes)
            }
        }

        viewModel.loadStorageData()

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        btnClearCache?.setOnClickListener {
            showConfirmationDialog(
                title = "Clear Cache",
                message = "Clear all app cache?"
            ) {
                viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    requireContext().cacheDir.deleteRecursively()
                    com.bumptech.glide.Glide.get(requireContext()).clearDiskCache()
                    launch(kotlinx.coroutines.Dispatchers.Main) {
                        com.bumptech.glide.Glide.get(requireContext()).clearMemory()
                        AlertDialog.Builder(requireContext())
                            .setMessage("Cache cleared successfully.")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
        }

        btnDeleteAll?.setOnClickListener {
            showConfirmationDialog(
                title = "Confirm Deletion",
                message = "Delete all sticker packs? This action cannot be undone."
            ) {
                viewModel.deleteAll()
            }
        }
    }

    private fun showConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
