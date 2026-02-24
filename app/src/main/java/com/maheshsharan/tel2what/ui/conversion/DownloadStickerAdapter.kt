package com.maheshsharan.tel2what.ui.conversion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.maheshsharan.tel2what.R
import com.maheshsharan.tel2what.data.local.entity.StickerEntity
import java.io.File

class DownloadStickerAdapter : RecyclerView.Adapter<DownloadStickerAdapter.StickerViewHolder>() {

    private var stickers = listOf<StickerEntity>()

    fun submitList(newList: List<StickerEntity>) {
        stickers = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_download_sticker, parent, false)
        return StickerViewHolder(view)
    }

    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        holder.bind(stickers[position])
    }

    override fun getItemCount(): Int = stickers.size

    class StickerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imgSticker: ImageView = itemView.findViewById(R.id.imgSticker)
        private val layoutStatus: LinearLayout = itemView.findViewById(R.id.layoutStatus)
        private val txtStatus: TextView = itemView.findViewById(R.id.txtStatus)
        private val progressSticker: ProgressBar = itemView.findViewById(R.id.progressSticker)

        fun bind(sticker: StickerEntity) {
            when (sticker.status) {
                "DOWNLOADING" -> {
                    layoutStatus.visibility = View.VISIBLE
                    progressSticker.visibility = View.VISIBLE
                    txtStatus.text = "Down..."
                    imgSticker.setImageDrawable(null)
                }
                "CONVERTING" -> {
                    layoutStatus.visibility = View.VISIBLE
                    progressSticker.visibility = View.VISIBLE
                    txtStatus.text = "Conv..."
                    imgSticker.setImageDrawable(null)
                }
                "READY" -> {
                    layoutStatus.visibility = View.GONE
                    if (sticker.imageFile.isNotEmpty()) {
                        Glide.with(itemView.context)
                            .load(File(sticker.imageFile))
                            .into(imgSticker)
                    }
                }
                "FAILED" -> {
                    layoutStatus.visibility = View.VISIBLE
                    progressSticker.visibility = View.GONE
                    txtStatus.text = "Failed"
                    txtStatus.setTextColor(android.graphics.Color.RED)
                }
            }
        }
    }
}
