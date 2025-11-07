package com.example.amtpi

import android.content.Intent
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

class PostsAdapter(
    private val posts: List<Post>,
    private val onPostLongClickListener: (Post) -> Unit,
    private val onLikeClickListener: (Post) -> Unit
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.findViewById(R.id.post_username)
        val content: TextView = view.findViewById(R.id.post_content_text)
        val timestamp: TextView = view.findViewById(R.id.post_timestamp)
        val postImage: ImageView = view.findViewById(R.id.post_image)
        val likeButton: ImageButton = view.findViewById(R.id.like_button)
        val likeCount: TextView = view.findViewById(R.id.like_count)
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



        if (post.imageUrls.isNotEmpty()) {
            try {

                val imageBytes = Base64.decode(post.imageUrls, Base64.DEFAULT)


                Glide.with(holder.itemView.context)
                    .load(imageBytes)
                    .into(holder.postImage)

                holder.postImage.visibility = View.VISIBLE
            } catch (e: IllegalArgumentException) {

                holder.postImage.visibility = View.GONE
            }
        } else {
            holder.postImage.visibility = View.GONE
        }

        val likesCount = post.likedBy.size
        val context = holder.itemView.context
        holder.likeCount.text = context.getString(R.string.likes_count, likesCount)
        holder.likeCount.visibility = if (likesCount > 0) View.VISIBLE else View.INVISIBLE


        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && post.likedBy.contains(currentUser.uid)) {

            holder.likeButton.setImageResource(R.drawable.baseline_favorite_24)
        } else {

            holder.likeButton.setImageResource(R.drawable.outline_favorite_24)
        }

        holder.likeButton.setOnClickListener {
            onLikeClickListener(post)
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
