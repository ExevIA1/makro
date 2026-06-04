package com.makro.app

import android.graphics.BitmapFactory
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class TargetAdapter(
    private var files: List<File>,
    private val onLongPress: (File) -> Unit
) : RecyclerView.Adapter<TargetAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(android.R.id.icon)
        val name: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.activity_list_item, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val file = files[position]
        holder.name.text = file.name
        holder.img.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
        holder.img.layoutParams.width  = 80
        holder.img.layoutParams.height = 80

        // Uzun basınca sil
        holder.itemView.setOnLongClickListener {
            onLongPress(file)
            true
        }
    }

    override fun getItemCount() = files.size

    fun updateList(newFiles: List<File>) {
        files = newFiles
        notifyDataSetChanged()
    }
}
