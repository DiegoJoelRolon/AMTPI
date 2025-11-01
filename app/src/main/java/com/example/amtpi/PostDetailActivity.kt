package com.example.amtpi

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class PostDetailActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var commentEditText: EditText
    private lateinit var sendCommentButton: ImageButton

    private lateinit var commentsAdapter: CommentsAdapter
    private val commentsList = mutableListOf<Comment>()
    private var postId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        commentsRecyclerView = findViewById(R.id.comments_recycler_view)
        commentEditText = findViewById(R.id.comment_edit_text)
        sendCommentButton = findViewById(R.id.send_comment_button)


        commentsAdapter = CommentsAdapter(commentsList) { comment ->
            handleCommentLike(comment)
        }
        commentsRecyclerView.adapter = commentsAdapter
        commentsRecyclerView.isNestedScrollingEnabled = false

        val usernameTextView = findViewById<TextView>(R.id.detail_post_username)
        val contentTextView = findViewById<TextView>(R.id.detail_post_content)
        val timestampTextView = findViewById<TextView>(R.id.detail_post_timestamp)
        val imageView = findViewById<ImageView>(R.id.detail_post_image)

        val post: Post? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("POST_DETAIL", Post::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("POST_DETAIL")
        }

        if (post != null) {
            postId = post.id
            usernameTextView.text = post.username
            contentTextView.text = post.content

            post.timestamp?.let {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                timestampTextView.text = sdf.format(it)
            }

            if (post.imageUrls.isNotEmpty()) {
                imageView.visibility = View.VISIBLE
                try {
                    val imageBytes = Base64.decode(post.imageUrls, Base64.DEFAULT)
                    Glide.with(this)
                        .load(imageBytes)
                        .into(imageView)
                } catch (e: IllegalArgumentException) {
                    imageView.visibility = View.GONE
                }
            } else {
                imageView.visibility = View.GONE
            }

            postId?.let { id ->
                loadComments(id)
                sendCommentButton.setOnClickListener {
                    sendComment(id)
                }
            }
        } else {
            Toast.makeText(this, "Error al cargar el post.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadComments(postId: String) {
        db.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("PostDetailActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val oldListSize = commentsList.size
                commentsList.clear()
                commentsAdapter.notifyItemRangeRemoved(0, oldListSize)

                snapshots?.forEach { doc ->
                    val comment = doc.toObject(Comment::class.java).apply {
                        id = doc.id
                    }
                    commentsList.add(comment)
                }
                commentsAdapter.notifyItemRangeInserted(0, commentsList.size)
            }
    }


    private fun handleCommentLike(comment: Comment) {
        val currentUser = auth.currentUser
        if (currentUser == null || postId == null || comment.id == null) {
            Toast.makeText(this, "No se pudo procesar la acción.", Toast.LENGTH_SHORT).show()
            return
        }

        val commentRef = db.collection("posts").document(postId!!).collection("comments").document(comment.id!!)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(commentRef)
            val likedBy = snapshot.get("likedBy") as? MutableList<String> ?: mutableListOf()

            if (likedBy.contains(currentUser.uid)) {

                transaction.update(commentRef, "likedBy", FieldValue.arrayRemove(currentUser.uid))
            } else {

                transaction.update(commentRef, "likedBy", FieldValue.arrayUnion(currentUser.uid))
            }
            null
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error al actualizar el 'me gusta'.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendComment(postId: String) {
        val commentText = commentEditText.text.toString().trim()
        val currentUser = auth.currentUser

        if (commentText.isEmpty()) {
            Toast.makeText(this, "El comentario no puede estar vacío.", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesión para comentar.", Toast.LENGTH_SHORT).show()
            return
        }

        commentEditText.isEnabled = false
        sendCommentButton.isEnabled = false
        sendCommentButton.alpha = 0.5f

        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(commentEditText.windowToken, 0)

        val currentUserName = currentUser.displayName ?: "Usuario Anónimo"

        val tempComment = Comment(
            commentText = commentText,
            userId = currentUser.uid,
            username = currentUserName
        )

        commentEditText.text.clear()

        db.collection("posts").document(postId).collection("comments")
            .add(tempComment)
            .addOnSuccessListener {
                commentEditText.isEnabled = true
                sendCommentButton.isEnabled = true
                sendCommentButton.alpha = 1.0f
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al publicar. Inténtalo de nuevo.", Toast.LENGTH_LONG).show()
                commentEditText.setText(commentText)
                commentEditText.isEnabled = true
                sendCommentButton.isEnabled = true
                sendCommentButton.alpha = 1.0f
            }
    }
}
