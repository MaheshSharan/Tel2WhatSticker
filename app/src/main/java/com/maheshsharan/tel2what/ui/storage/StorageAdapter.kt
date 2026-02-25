package com.maheshsharan.tel2what.ui.storage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.maheshsharan.tel2what.R
import java.io.File

class StorageAdapter(
    private val onClearCacheClick: (String, String) -> Unit, // packId, packName
    private val onDeleteClick: (String, String) -> Unit // packId, packName
) : RecyclerView.Adapter<StorageAdapter.StorageViewHolder>() {

    private var items = listOf<PackStorageInfo>()

    fun submitList(newList: List<PackStorageInfo>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StorageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_storage_pack, parent, false)
        return StorageViewHolder(view, onClearCacheClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: StorageViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class StorageViewHolder(
        itemView: View,
        private val onClearCache: (String, String) -> Unit,
        private val onDelete: (String, String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val imgTrayPreview: ImageView = itemView.findViewById(R.id.imgTrayPreview)
        private val txtPackName: TextView = itemView.findViewById(R.id.txtPackName)
        private val txtPackDetails: TextView = itemView.findViewById(R.id.txtPackDetails)
        private val btnClearPackCache: ImageView = itemView.findViewById(R.id.btnClearPackCache)
        private val btnDeletePack: ImageView = itemView.findViewById(R.id.btnDeletePack)

        fun bind(info: PackStorageInfo) {
            val pack = info.pack
            
            Glide.with(itemView.context)
                .load(File(pack.trayImageFile))
                .into(imgTrayPreview)

            txtPackName.text = pack.name
            txtPackDetails.text = "${info.totalStickers} Stickers • ${formatFileSize(info.sizeBytes)}"

            btnClearPackCache.setOnClickListener {
                onClearCache(pack.identifier, pack.name)
            }

            btnDeletePack.setOnClickListener {
                onDelete(pack.identifier, pack.name)
            }
        }
    }
}
