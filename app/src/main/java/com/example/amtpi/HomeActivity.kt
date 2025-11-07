package com.example.amtpi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postsAdapter: PostsAdapter
    private val postList = mutableListOf<Post>()

    private val NOTIFICATION_CHANNEL_ID = "like_notifications"
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 101
    private val LIKE_THRESHOLD = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val ctx = applicationContext
        org.osmdroid.config.Configuration.getInstance().load(ctx, androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx))
        org.osmdroid.config.Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        postsRecyclerView = findViewById(R.id.posts_recyclerview)
        postsRecyclerView.layoutManager = LinearLayoutManager(this)

        postsAdapter = PostsAdapter(postList,
            onPostLongClickListener = { post -> showDeleteConfirmationDialog(post) },
            onLikeClickListener = { post -> toggleLike(post) }
        )
        postsRecyclerView.adapter = postsAdapter

        val btnCerrarSesion = findViewById<Button>(R.id.btn_cerrar_sesion)
        btnCerrarSesion.setOnClickListener {
            val intent = Intent(this, PersonalProfileActivity::class.java)
            startActivity(intent)
        }

        val btnNuevoPdi = findViewById<Button>(R.id.btn_pdi)
        btnNuevoPdi.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

        val btnUserProfile = findViewById<Button>(R.id.btn_user_profile)
        btnUserProfile.setOnClickListener {

            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }


        createNotificationChannel()


        loadPosts()
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndThenCheckPosts()
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
                Log.d("NotificationCheck", "Permiso de notificación denegado")
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

    private fun showDeleteConfirmationDialog(post: Post) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_confirmation_title))
            .setMessage(getString(R.string.delete_confirmation_message))
            .setPositiveButton(getString(R.string.delete_button)) { _, _ ->
                deletePost(post)
            }.setNegativeButton(android.R.string.cancel, null)

            .show()
    }
    private fun deletePost(post: Post) {
        val postId = post.id
        if (postId == null) {
            Toast.makeText(this, "No se puede borrar el posteo (ID no encontrado).", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("posts").document(postId).delete()

            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al borrar el posteo: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.w("HomeActivity", "Error deleting document", e)
            }
    }

    private fun loadPosts() {
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("HomeActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }

                postList.clear()
                for (doc in snapshots!!) {
                    val post = doc.toObject(Post::class.java).copy(id = doc.id)
                    postList.add(post)
                }
                postsAdapter.notifyDataSetChanged()
            }
    }


    private fun toggleLike(post: Post) {
        val currentUser = auth.currentUser ?: return
        val postRef = db.collection("posts").document(post.id!!)


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
            Log.w("ToggleLike", "Error en la transacción de like", e)
            Toast.makeText(this, "Error al procesar el 'me gusta'", Toast.LENGTH_SHORT).show()
        }

    }
}
