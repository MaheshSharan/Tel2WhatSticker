package com.maheshsharan.tel2what.ui.selection

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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

class StickerSelectionFragment : Fragment(R.layout.fragment_sticker_selection) {

    private companion object {
        private const val TAG = "Tel2What"
    }

    private lateinit var viewModel: SelectionViewModel
    private lateinit var adapter: SelectableStickerAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        val repository = StickerRepository(database.stickerDao(), TelegramBotApi(), FileDownloader())
        val factory = SelectionViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[SelectionViewModel::class.java]

        val btnBack: ImageView = view.findViewById(R.id.btnBack)
        val btnContinue: Button = view.findViewById(R.id.btnContinue)
        val btnSelectAll: Button = view.findViewById(R.id.btnSelectAll)
        val txtSelectionCount: TextView = view.findViewById(R.id.txtSelectionCount)
        val recyclerStickers: RecyclerView = view.findViewById(R.id.recyclerStickers)

        val packName = arguments?.getString("packName") ?: return
        Log.i(TAG, "SelectionUI:onViewCreated packName=$packName")

        adapter = SelectableStickerAdapter { stickerId ->
            viewModel.toggleSelection(stickerId)
        }
        recyclerStickers.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.stickers.collect { items ->
                Log.i(TAG, "SelectionUI:list size=${items.size}")
                adapter.submitList(items)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedCount.collect { count ->
                Log.i(TAG, "SelectionUI:selectedCount=$count")
                txtSelectionCount.text = "$count / 30 selected"
                btnContinue.text = "Continue ($count)"
                btnContinue.isEnabled = count in 3..30
                btnSelectAll.text = if (count > 0) "Deselect All" else "Select All"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is SelectionEvent.ShowMixedPackPrompt -> {
                        val message = if (event.targetIsAnimated) {
                            "Selecting this animated sticker will deselect all currently selected static stickers. Proceed?"
                        } else {
                            "Selecting this static sticker will deselect all currently selected animated stickers. Proceed?"
                        }
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Select one category only")
                            .setMessage(message)
                            .setPositiveButton("Yes") { _, _ ->
                                viewModel.forceToggleSelection(event.stickerId, event.targetIsAnimated)
                            }
                            .setNegativeButton("No", null)
                            .show()
                    }
                }
            }
        }

        viewModel.loadStickers(packName)

        btnSelectAll.setOnClickListener {
            viewModel.toggleSelectAllOrNone()
        }

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        btnContinue.setOnClickListener {
            val selectedIds = viewModel.getSelectedStickerIds()
            if (selectedIds.size in 3..30) {
                val bundle = Bundle().apply {
                    putString("packName", packName)
                    putLongArray("selectedIds", selectedIds.toLongArray())
                }
                findNavController().navigate(R.id.action_stickerSelectionFragment_to_trayIconSelectionFragment, bundle)
            } else {
                Toast.makeText(requireContext(), "You must select between 3 and 30 stickers", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
