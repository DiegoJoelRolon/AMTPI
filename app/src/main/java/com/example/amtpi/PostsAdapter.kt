package com.example.amtpi

import android.content.Intent
import android.util.Base64 // <-- Asegúrate de que esta importación esté presente
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class PostsAdapter(
    private val posts: List<Post>,
    private val onPostLongClickListener: (Post) -> Unit
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.findViewById(R.id.post_username)
        val content: TextView = view.findViewById(R.id.post_content_text)
        val timestamp: TextView = view.findViewById(R.id.post_timestamp)
        val postImage: ImageView = view.findViewById(R.id.post_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.username.text = post.username
        holder.content.text = post.content
        post.timestamp?.let {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            holder.timestamp.text = sdf.format(it)
        }

        // --- INICIO DE CÓDIGO MODIFICADO ---

        // Cargar la imagen desde la cadena Base64
        if (post.imageUrls.isNotEmpty()) {
            try {
                // Decodifica la cadena Base64 a un array de bytes
                val imageBytes = Base64.decode(post.imageUrls, Base64.DEFAULT)

                // Glide puede cargar un array de bytes directamente
                Glide.with(holder.itemView.context)
                    .load(imageBytes)
                    .into(holder.postImage)

                holder.postImage.visibility = View.VISIBLE
            } catch (e: IllegalArgumentException) {
                // Esto puede suceder si la cadena Base64 es inválida
                holder.postImage.visibility = View.GONE
            }
        } else {
            holder.postImage.visibility = View.GONE
        }

        // --- FIN DE CÓDIGO MODIFICADO ---

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, PostDetailActivity::class.java).apply {
                putExtra("POST_DETAIL", post)
            }
            context.startActivity(intent)
        }

        holder.itemView.setOnLongClickListener {
            onPostLongClickListener(post)
            true
        }
    }

    override fun getItemCount() = posts.size
}
