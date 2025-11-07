package com.example.amtpi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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


    private lateinit var likeButton: ImageButton
    private lateinit var likeCountTextView: TextView

    private lateinit var commentsAdapter: CommentsAdapter
    private val commentsList = mutableListOf<Comment>()
    private var postId: String? = null

    private val NOTIFICATION_CHANNEL_ID = "like_notifications"
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 101
    private val LIKE_THRESHOLD = 1


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


        likeButton = findViewById(R.id.detail_like_button)
        likeCountTextView = findViewById(R.id.detail_like_count)

        commentsAdapter = CommentsAdapter(commentsList,
            onLikeClickListener = { comment -> handleCommentLike(comment) },
            onEditClickListener = { comment -> showEditCommentDialog(comment) }
        )
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

                setupPostListener(id)
                likeButton.setOnClickListener {
                    togglePostLike(id)
                }
                sendCommentButton.setOnClickListener {
                    sendComment(id)
                }
            }
        } else {
            Toast.makeText(this, "Error al cargar el post.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndThenCheckPosts()
    }

    private fun setupPostListener(postId: String) {
        val postRef = db.collection("posts").document(postId)
        postRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("PostDetailActivity", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val post = snapshot.toObject(Post::class.java)
                post?.let { updateLikeUI(it) }
            } else {
                Log.d("PostDetailActivity", "Current data: null")
            }
        }
    }

    private fun updateLikeUI(post: Post) {
        val currentUser = auth.currentUser
        val likesCount = post.likedBy.size
        likeCountTextView.text = getString(R.string.likes_count, likesCount)
        likeCountTextView.visibility = if (likesCount > 0) View.VISIBLE else View.INVISIBLE

        if (currentUser != null && post.likedBy.contains(currentUser.uid)) {
            likeButton.setImageResource(R.drawable.baseline_favorite_24)
        } else {
            likeButton.setImageResource(R.drawable.outline_favorite_24)
        }
    }


    private fun togglePostLike(postId: String) {
        val currentUser = auth.currentUser ?: return
        val postRef = db.collection("posts").document(postId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val currentLikedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()

            if (currentLikedBy.contains(currentUser.uid)) {

                transaction.update(postRef, "likedBy", FieldValue.arrayRemove(currentUser.uid))
                transaction.update(postRef, "likedByCount", FieldValue.increment(-1))
            } else {

                transaction.update(postRef, "likedBy", FieldValue.arrayUnion(currentUser.uid))
                transaction.update(postRef, "likedByCount", FieldValue.increment(1))
            }
            null
        }.addOnFailureListener { e ->
            Log.w("ToggleLike", "Error en la transacción de like del post", e)
            Toast.makeText(this, "Error al procesar el 'me gusta'", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditCommentDialog(comment: Comment) {
        val editText = EditText(this).apply {
            setText(comment.commentText)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(50, 20, 50, 20)
            }
        }



        AlertDialog.Builder(this)
            .setTitle(getString(R.string.edit_comment))
            .setView(editText)
            .setPositiveButton(getString(R.string.save_changes)) { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty() && newText != comment.commentText) {
                    updateComment(comment, newText)
                } else if (newText.isEmpty()) {

                    Toast.makeText(this, "El comentario no puede estar vacío.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null) // Usando un recurso estándar de Android para "Cancelar"
            .show()
    }

    private fun updateComment(comment: Comment, newText: String) {
        if (postId == null || comment.id == null) {
            Toast.makeText(this, "Error al editar el comentario.", Toast.LENGTH_SHORT).show()
            return
        }
        val commentRef = db.collection("posts").document(postId!!).collection("comments").document(comment.id!!)

        commentRef.update("commentText", newText)
            .addOnSuccessListener {
                Toast.makeText(this, "Comentario actualizado.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("PostDetailActivity", "Error updating comment", e)
            }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notificaciones de Likes"
            val descriptionText = "Notificaciones para cuando un post alcanza muchos likes"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkPermissionsAndThenCheckPosts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            } else {

                checkForPopularPosts()
            }
        } else {

            checkForPopularPosts()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkForPopularPosts()
            } else {
                Toast.makeText(this, "No recibirás notificaciones de 'me gusta'.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkForPopularPosts() {
        val currentUser = auth.currentUser ?: return

        val notifiedPostsPrefs = getSharedPreferences("notified_posts", Context.MODE_PRIVATE)

        db.collection("posts")
            .whereEqualTo("userId", currentUser.uid)
            .whereGreaterThanOrEqualTo("likedByCount", LIKE_THRESHOLD)
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val post = doc.toObject(Post::class.java).copy(id = doc.id)
                    val wasNotified = notifiedPostsPrefs.getBoolean(post.id, false)

                    if (!wasNotified) {
                        Log.d("NotificationCheck", "Post ${post.id} superó el umbral. ¡Enviando notificación!")
                        val notificationId = post.id?.hashCode() ?: doc.id.hashCode()
                        sendPopularPostNotification(post, notificationId)

                        with(notifiedPostsPrefs.edit()) {
                            putBoolean(post.id, true)
                            apply()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("NotificationCheck", "Error al buscar posts populares", e)
            }
    }

    private fun sendPopularPostNotification(post: Post, notificationId: Int) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return
        }
        val title = getString(R.string.popular_post_notification_title)
        val contentText = getString(
            R.string.popular_post_notification_text,
            post.content.take(20),
            post.likedBy.size
        )
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, builder.build())
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
