package com.maheshsharan.tel2what.ui.export

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.maheshsharan.tel2what.R
import com.maheshsharan.tel2what.data.local.AppDatabase
import com.maheshsharan.tel2what.data.network.FileDownloader
import com.maheshsharan.tel2what.data.network.TelegramBotApi
import com.maheshsharan.tel2what.data.repository.StickerRepository
import kotlinx.coroutines.launch
import java.io.File

class ExportFragment : Fragment(R.layout.fragment_export) {

    private lateinit var viewModel: ExportViewModel
    
    private val whatsappLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        // WhatsApp returns, navigate home
        findNavController().navigate(R.id.action_exportFragment_to_homeFragment)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val packName = arguments?.getString("packName") ?: return

        val database = AppDatabase.getDatabase(requireContext())
        val repository = StickerRepository(database.stickerDao(), TelegramBotApi(), FileDownloader())
        val factory = ExportViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ExportViewModel::class.java]

        val btnBack: ImageView = view.findViewById(R.id.btnBack)
        val btnExport: Button = view.findViewById(R.id.btnExport)
        val etPackName: EditText = view.findViewById(R.id.etPackName)
        val etAuthorName: EditText = view.findViewById(R.id.etAuthorName)
        val imgTrayPreview: ImageView = view.findViewById(R.id.imgTrayPreview)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pack.collect { pack ->
                if (pack != null) {
                    if (etPackName.text.isEmpty()) {
                        etPackName.setText(pack.name)
                    }
                    if (pack.trayImageFile.isNotEmpty()) {
                        Glide.with(requireContext())
                            .load(File(pack.trayImageFile))
                            .into(imgTrayPreview)
                    }
                }
            }
        }

        viewModel.loadPackDetails(packName)

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        btnExport.setOnClickListener {
            val name = etPackName.text.toString().trim()
            val author = etAuthorName.text.toString().trim()

            // Validate required fields before export
            if (name.isEmpty()) {
                etPackName.error = "Pack name is required"
                etPackName.requestFocus()
                return@setOnClickListener
            }
            if (author.isEmpty()) {
                etAuthorName.error = "Author name is required"
                etAuthorName.requestFocus()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val success = viewModel.updatePackDetailsAndSave(name, author)
                if (success) {
                    addStickerPackToWhatsApp(packName, name)
                } else {
                    Toast.makeText(requireContext(), "Error saving details", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addStickerPackToWhatsApp(identifier: String, stickerPackName: String) {
        val intent = Intent()
        intent.action = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
        intent.putExtra("sticker_pack_id", identifier)
        intent.putExtra("sticker_pack_authority", "com.maheshsharan.tel2what.provider")
        intent.putExtra("sticker_pack_name", stickerPackName)
        
        try {
            whatsappLauncher.launch(intent)
            Toast.makeText(requireContext(), "Opening WhatsApp...", Toast.LENGTH_SHORT).show()
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "WhatsApp is not installed", Toast.LENGTH_LONG).show()
        }
    }
}
