package com.maheshsharan.tel2what.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.maheshsharan.tel2what.R
import com.maheshsharan.tel2what.data.local.entity.StickerPackEntity

class RecentPacksAdapter(private val onPackClicked: (StickerPackEntity) -> Unit) :
    RecyclerView.Adapter<RecentPacksAdapter.PackViewHolder>() {

    private var packs = listOf<StickerPackEntity>()

    fun submitList(newPacks: List<StickerPackEntity>) {
        packs = newPacks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_pack, parent, false)
        return PackViewHolder(view, onPackClicked)
    }

    override fun onBindViewHolder(holder: PackViewHolder, position: Int) {
        holder.bind(packs[position])
    }

    override fun getItemCount(): Int = packs.size

    class PackViewHolder(
        itemView: View,
        val onPackClicked: (StickerPackEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val imgTrayIcon: ImageView = itemView.findViewById(R.id.imgTrayIcon)
        private val txtPackName: TextView = itemView.findViewById(R.id.txtPackName)
        private val txtStickerCount: TextView = itemView.findViewById(R.id.txtStickerCount)

        fun bind(pack: StickerPackEntity) {
            txtPackName.text = pack.name
            // We'll calculate sticker count accurately later, for now just show generic text if not tracked
            txtStickerCount.text = "Saved Pack"

            // Ensure tray_image is properly loaded if it exists
            if (pack.trayImageFile.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(pack.trayImageFile) // Path saved locally
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(imgTrayIcon)
            } else {
                imgTrayIcon.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            itemView.setOnClickListener { onPackClicked(pack) }
        }
    }
}
