package com.example.amtpi

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView // Importa ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // Importa Glide
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
        // Referencia al nuevo ImageView
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

        // Cargar la imagen usando Glide
        if (post.imageUrls.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(post.imageUrls)
                .into(holder.postImage)
            holder.postImage.visibility = View.VISIBLE
        } else {
            holder.postImage.visibility = View.GONE
        }

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
