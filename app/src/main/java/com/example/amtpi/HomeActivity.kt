package com.example.amtpi

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
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

        loadPosts()
    }


    private fun showDeleteConfirmationDialog(post: Post) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar borrado")
            .setMessage("¿Estás seguro de que quieres borrar este posteo?")
            .setPositiveButton("Borrar") { _, _ ->
                deletePost(post)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    private fun deletePost(post: Post) {
        val postId = post.id
        if (postId == null) {
            Toast.makeText(this, "No se puede borrar el posteo (ID no encontrado).", Toast.LENGTH_SHORT).show()
            return
        }


        db.collection("posts").document(postId).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Posteo borrado exitosamente.", Toast.LENGTH_SHORT).show()

            }
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
                if (postList.isNotEmpty()) {

                }
            }
    }

    private fun toggleLike(post: Post) {
        val currentUser = auth.currentUser ?: return
        val postRef = db.collection("posts").document(post.id!!)

        if (post.likedBy.contains(currentUser.uid)) {

            postRef.update("likedBy", FieldValue.arrayRemove(currentUser.uid))
        } else {

            postRef.update("likedBy", FieldValue.arrayUnion(currentUser.uid))
        }
    }


}
