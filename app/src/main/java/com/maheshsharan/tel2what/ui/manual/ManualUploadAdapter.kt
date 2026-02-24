package com.maheshsharan.tel2what.ui.manual

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.maheshsharan.tel2what.R

class ManualUploadAdapter(
    private val onRemoveClick: (Uri) -> Unit
) : RecyclerView.Adapter<ManualUploadAdapter.UploadViewHolder>() {

    private var items = listOf<ManualUploadItem>()
    private var isProcessing = false

    fun submitList(newList: List<ManualUploadItem>, processing: Boolean) {
        items = newList
        isProcessing = processing
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UploadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manual_upload, parent, false)
        return UploadViewHolder(view, onRemoveClick)
    }

    override fun onBindViewHolder(holder: UploadViewHolder, position: Int) {
        holder.bind(items[position], isProcessing)
    }

    override fun getItemCount(): Int = items.size

    class UploadViewHolder(
        itemView: View,
        private val onRemove: (Uri) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val imgUploadSticker: ImageView = itemView.findViewById(R.id.imgUploadSticker)
        private val layoutProcessing: LinearLayout = itemView.findViewById(R.id.layoutProcessing)
        private val imgSuccess: ImageView = itemView.findViewById(R.id.imgSuccess)
        private val btnRemove: ImageView = itemView.findViewById(R.id.btnRemove)

        fun bind(item: ManualUploadItem, isProcessingGlobal: Boolean) {
            Glide.with(itemView.context)
                .load(item.uri)
                .into(imgUploadSticker)

            when (item.status) {
                "IDLE" -> {
                    layoutProcessing.visibility = View.GONE
                    imgSuccess.visibility = View.GONE
                    btnRemove.visibility = if (isProcessingGlobal) View.GONE else View.VISIBLE
                }
                "PROCESSING" -> {
                    layoutProcessing.visibility = View.VISIBLE
                    imgSuccess.visibility = View.GONE
                    btnRemove.visibility = View.GONE
                }
                "READY" -> {
                    layoutProcessing.visibility = View.GONE
                    imgSuccess.visibility = View.VISIBLE
                    btnRemove.visibility = View.GONE
                }
                "FAILED" -> {
                    layoutProcessing.visibility = View.GONE
                    imgSuccess.visibility = View.GONE
                    btnRemove.visibility = View.VISIBLE
                    // You might want to tint it red or show an error icon
                }
            }

            btnRemove.setOnClickListener {
                onRemove(item.uri)
            }
        }
    }
}
