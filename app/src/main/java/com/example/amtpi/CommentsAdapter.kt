package com.example.amtpi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

class CommentsAdapter(
    private val comments: List<Comment>,
    private val onLikeClickListener: (Comment) -> Unit
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {


    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val usernameTextView: TextView = view.findViewById(R.id.comment_username)
        val commentTextView: TextView = view.findViewById(R.id.comment_text)
        val timestampTextView: TextView = view.findViewById(R.id.comment_timestamp)
        val likeButton: ImageButton = view.findViewById(R.id.comment_like_button)
        val likeCount: TextView = view.findViewById(R.id.comment_like_count)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(view)
    }


    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        val currentUser = FirebaseAuth.getInstance().currentUser

        holder.usernameTextView.text = comment.username
        holder.commentTextView.text = comment.commentText
        comment.timestamp?.let {
            val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
            holder.timestampTextView.text = sdf.format(it)
        }


        val likesCount = comment.likedBy.size
        holder.likeCount.text = "$likesCount Me gusta"
        holder.likeCount.visibility = if (likesCount > 0) View.VISIBLE else View.INVISIBLE

        if (currentUser != null && comment.likedBy.contains(currentUser.uid)) {
            holder.likeButton.setImageResource(R.drawable.baseline_favorite_24)
        } else {
            holder.likeButton.setImageResource(R.drawable.outline_favorite_24)
        }

        holder.likeButton.setOnClickListener {
            onLikeClickListener(comment)
        }
    }


    override fun getItemCount() = comments.size
}
