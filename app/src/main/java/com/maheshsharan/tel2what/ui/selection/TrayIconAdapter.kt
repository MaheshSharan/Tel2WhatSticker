package com.maheshsharan.tel2what.ui.selection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.maheshsharan.tel2what.R
import com.maheshsharan.tel2what.data.local.entity.StickerEntity
import java.io.File

class TrayIconAdapter(
    private val onIconSelected: (String) -> Unit
) : RecyclerView.Adapter<TrayIconAdapter.TrayIconViewHolder>() {

    private var stickers = listOf<StickerEntity>()
    private var selectedPath: String? = null
    private var colorPrimary: Int = 0
    private var colorTransparent: Int = 0

    fun submitList(newList: List<StickerEntity>, currentSelectedPath: String?) {
        stickers = newList
        selectedPath = currentSelectedPath
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrayIconViewHolder {
        if (colorPrimary == 0) {
            colorPrimary = android.graphics.Color.parseColor("#128C7E") // WhatsApp green fallback
            colorTransparent = android.graphics.Color.TRANSPARENT
        }

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tray_icon, parent, false)
        return TrayIconViewHolder(view, onIconSelected, colorPrimary, colorTransparent)
    }

    override fun onBindViewHolder(holder: TrayIconViewHolder, position: Int) {
        val entity = stickers[position]
        holder.bind(entity, entity.imageFile == selectedPath)
    }

    override fun getItemCount(): Int = stickers.size

    class TrayIconViewHolder(
        itemView: View,
        private val onSelected: (String) -> Unit,
        private val colorPrimary: Int,
        private val colorTransparent: Int
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardTrayIcon: MaterialCardView = itemView.findViewById(R.id.cardTrayIcon)
        private val imgTrayIcon: ImageView = itemView.findViewById(R.id.imgTrayIcon)

        fun bind(entity: StickerEntity, isSelected: Boolean) {
            Glide.with(itemView.context)
                .load(File(entity.imageFile))
                .into(imgTrayIcon)

            if (isSelected) {
                cardTrayIcon.strokeColor = colorPrimary
            } else {
                cardTrayIcon.strokeColor = colorTransparent
            }

            itemView.setOnClickListener {
                onSelected(entity.imageFile)
            }
        }
    }
}
