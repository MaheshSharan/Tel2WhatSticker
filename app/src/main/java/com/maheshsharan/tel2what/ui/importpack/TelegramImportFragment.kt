package com.maheshsharan.tel2what.ui.importpack

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.maheshsharan.tel2what.R
import com.maheshsharan.tel2what.data.local.AppDatabase
import com.maheshsharan.tel2what.data.network.FileDownloader
import com.maheshsharan.tel2what.data.network.TelegramBotApi
import com.maheshsharan.tel2what.data.repository.StickerRepository
import com.maheshsharan.tel2what.data.network.model.previewFileId
import com.maheshsharan.tel2what.ui.custom.ShimmerDrawable
import android.graphics.Color
import kotlinx.coroutines.launch

class TelegramImportFragment : Fragment(R.layout.fragment_telegram_import) {

    private lateinit var viewModel: TelegramImportViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup ViewModel
        val database = AppDatabase.getDatabase(requireContext())
        val repository = StickerRepository(database.stickerDao(), TelegramBotApi(), FileDownloader())
        val factory = TelegramImportViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TelegramImportViewModel::class.java]

        val btnBack: ImageView = view.findViewById(R.id.btnBack)
        val layoutStickerLink: com.google.android.material.textfield.TextInputLayout = view.findViewById(R.id.layoutStickerLink)
        val etStickerLink: TextInputEditText = view.findViewById(R.id.etStickerLink)
        
        val btnAction: Button = view.findViewById(R.id.btnAction)
        val progressLoading: android.widget.ProgressBar = view.findViewById(R.id.progressLoading)
        
        val cardPreview: MaterialCardView = view.findViewById(R.id.cardPreview)
        val imgPreviewThumb: ImageView = view.findViewById(R.id.imgPreviewThumb)
        val txtPackTitle: TextView = view.findViewById(R.id.txtPackTitle)
        val txtPackStickerCount: TextView = view.findViewById(R.id.txtPackStickerCount)

        // Hide preview initially until fetched
        cardPreview.visibility = View.GONE

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Paste functionality using TextInputLayout's end icon
        layoutStickerLink.setEndIconOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip() && clipboard.primaryClip?.itemCount!! > 0) {
                val pasteData = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                if (!pasteData.isNullOrEmpty()) {
                    etStickerLink.setText(pasteData)
                    layoutStickerLink.error = null
                    viewModel.fetchPackMetadata(pasteData)
                }
            } else {
                Toast.makeText(requireContext(), "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        }

        etStickerLink.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = etStickerLink.text.toString()
                if (text.isNotEmpty()) {
                    viewModel.fetchPackMetadata(text)
                }
            }
        }

        // Action button defaults to fetch if no metadata
        btnAction.setOnClickListener {
            val text = etStickerLink.text.toString()
            if (viewModel.importState.value !is ImportState.Success) {
                if (text.isNotEmpty()) {
                    layoutStickerLink.error = null
                    viewModel.fetchPackMetadata(text)
                } else {
                    layoutStickerLink.error = "Please enter a valid Telegram link"
                }
            }
        }

        // Observe State
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.importState.collect { state ->
                // Reset states
                progressLoading.visibility = View.GONE
                btnAction.isEnabled = true
                btnAction.alpha = 1f
                layoutStickerLink.error = null

                when (state) {
                    is ImportState.Idle -> {
                        cardPreview.visibility = View.GONE
                        btnAction.text = "Fetch Details"
                    }
                    is ImportState.Loading -> {
                        cardPreview.visibility = View.GONE
                        btnAction.text = ""
                        btnAction.isEnabled = false
                        btnAction.alpha = 0.5f
                        progressLoading.visibility = View.VISIBLE
                    }
                    is ImportState.Success -> {
                        cardPreview.visibility = View.VISIBLE
                        btnAction.text = "Download First 30 (${state.stickerSet.stickers.size} total)"

                        txtPackTitle.text = state.stickerSet.title
                        txtPackStickerCount.text = "${state.stickerSet.stickers.size} Stickers total"

                        val shimmer = createShimmerPlaceholder()
                        imgPreviewThumb.setImageDrawable(shimmer)

                        val targetFileId = state.stickerSet.previewFileId
                        if (targetFileId != null) {
                            viewModel.resolveTelegramFileUrl(targetFileId).observe(viewLifecycleOwner) { resolvedUrl ->
                                if (!resolvedUrl.isNullOrEmpty()) {
                                    com.bumptech.glide.Glide.with(this@TelegramImportFragment)
                                        .load(resolvedUrl)
                                        .placeholder(shimmer)
                                        .error(android.R.drawable.ic_menu_gallery)
                                        .centerCrop()
                                        .into(imgPreviewThumb)
                                } else {
                                    imgPreviewThumb.setImageResource(android.R.drawable.ic_menu_gallery)
                                }
                            }
                        } else {
                            imgPreviewThumb.setImageResource(android.R.drawable.ic_menu_gallery)
                        }

                        btnAction.setOnClickListener {
                            val bundle = Bundle().apply {
                                putString("packName", state.stickerSet.name)
                                putString("packTitle", state.stickerSet.title)
                                putInt("totalStickers", state.stickerSet.stickers.size)
                            }
                            findNavController().navigate(R.id.action_telegramImportFragment_to_downloadConversionFragment, bundle)
                        }
                    }
                    is ImportState.AlreadyDownloaded -> {
                        cardPreview.visibility = View.VISIBLE
                        btnAction.text = "Open Pack"

                        txtPackTitle.text = state.packTitle
                        txtPackStickerCount.text = "${state.stickerCount} Stickers saved"

                        val shimmer = createShimmerPlaceholder()
                        imgPreviewThumb.setImageDrawable(shimmer)

                        if (state.trayImageFile.isNotEmpty()) {
                            com.bumptech.glide.Glide.with(this@TelegramImportFragment)
                                .load(java.io.File(state.trayImageFile))
                                .placeholder(shimmer)
                                .error(android.R.drawable.ic_menu_gallery)
                                .centerCrop()
                                .into(imgPreviewThumb)
                        } else {
                            imgPreviewThumb.setImageResource(android.R.drawable.ic_menu_gallery)
                        }

                        btnAction.setOnClickListener {
                            val bundle = Bundle().apply {
                                putString("packName", state.packId)
                            }
                            findNavController().navigate(R.id.stickerSelectionFragment, bundle)
                        }
                    }
                    is ImportState.Error -> {
                        cardPreview.visibility = View.GONE
                        btnAction.text = "Fetch Details"
                        layoutStickerLink.error = state.message
                    }
                }
            }
        }
    }

    private fun createShimmerPlaceholder(): ShimmerDrawable {
        val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val base = if (isDark) Color.parseColor("#1C1C1C") else Color.parseColor("#E0E0E0")
        val highlight = if (isDark) Color.parseColor("#2D2D2D") else Color.parseColor("#F5F5F5")
        return ShimmerDrawable().apply {
            setColors(base, highlight)
            start()
        }
    }
}
