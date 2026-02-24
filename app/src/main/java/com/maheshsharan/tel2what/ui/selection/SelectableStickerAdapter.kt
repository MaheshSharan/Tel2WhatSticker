package com.maheshsharan.tel2what.ui.selection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.maheshsharan.tel2what.R
import java.io.File

class SelectableStickerAdapter(
    private val onStickerToggled: (Long) -> Unit
) : RecyclerView.Adapter<SelectableStickerAdapter.StickerViewHolder>() {

    private var stickers = listOf<SelectableSticker>()
    private var colorPrimary: Int = 0
    private var colorTransparent: Int = 0

    fun submitList(newList: List<SelectableSticker>) {
        stickers = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
        if (colorPrimary == 0) {
            colorPrimary = android.graphics.Color.parseColor("#128C7E") // WhatsApp green fallback
            colorTransparent = android.graphics.Color.TRANSPARENT
        }

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selectable_sticker, parent, false)
        return StickerViewHolder(view, onStickerToggled, colorPrimary, colorTransparent)
    }

    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        holder.bind(stickers[position])
    }

    override fun getItemCount(): Int = stickers.size

    class StickerViewHolder(
        itemView: View,
        private val onToggled: (Long) -> Unit,
        private val colorPrimary: Int,
        private val colorTransparent: Int
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardSticker: MaterialCardView = itemView.findViewById(R.id.cardSticker)
        private val imgSticker: ImageView = itemView.findViewById(R.id.imgSticker)
        private val imgSelected: ImageView = itemView.findViewById(R.id.imgSelected)

        fun bind(item: SelectableSticker) {
            Glide.with(itemView.context)
                .load(File(item.entity.imageFile))
                .into(imgSticker)

            if (item.isSelected) {
                cardSticker.strokeColor = colorPrimary
                imgSelected.visibility = View.VISIBLE
            } else {
                cardSticker.strokeColor = colorTransparent
                imgSelected.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onToggled(item.entity.id)
            }
        }
    }
}
