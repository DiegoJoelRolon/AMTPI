package com.example.amtpi

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PdiAdapter(private val pdiList: List<Pdi>) : RecyclerView.Adapter<PdiAdapter.PdiViewHolder>() {


    class PdiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.pdi_image)
        val nameTextView: TextView = view.findViewById(R.id.pdi_name)
        val descriptionTextView: TextView = view.findViewById(R.id.pdi_description)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdiViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.pdi_item, parent, false)
        return PdiViewHolder(view)
    }


    override fun onBindViewHolder(holder: PdiViewHolder, position: Int) {
        val pdi = pdiList[position]
        holder.nameTextView.text = pdi.name
        holder.descriptionTextView.text = pdi.description


        Glide.with(holder.itemView.context)
            .load(pdi.imageUrl)
            .into(holder.imageView)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, MapActivity::class.java).apply {

                putExtra("PDI_EXTRA", pdi)
            }
            context.startActivity(intent)
        }
    }


    override fun getItemCount() = pdiList.size
}
