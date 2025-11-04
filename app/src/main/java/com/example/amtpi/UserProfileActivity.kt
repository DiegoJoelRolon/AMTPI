package com.example.amtpi

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
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

class UserProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var userPostsRecyclerView: RecyclerView
    private lateinit var postsAdapter: PostsAdapter
    private val postList = mutableListOf<Post>()
    private lateinit var userProfileName: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_profile)
        // Asegúrate de que tu activity_user_profile.xml tiene el id "main" en el layout raíz
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        userProfileName = findViewById(R.id.user_profile_name)
        userPostsRecyclerView = findViewById(R.id.user_posts_recyclerview)
        userPostsRecyclerView.layoutManager = LinearLayoutManager(this)

        postsAdapter = PostsAdapter(postList,
            onPostLongClickListener = { post -> showDeleteConfirmationDialog(post) },
            onLikeClickListener = { post -> toggleLike(post) }
        )
        userPostsRecyclerView.adapter = postsAdapter

        loadUserProfile()
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        userProfileName.text = user.displayName ?: user.email
        loadUserPosts(user.uid)
    }

    private fun loadUserPosts(userId: String) {
        // Usar addSnapshotListener para actualizaciones en tiempo real
        db.collection("posts")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    // Este es el error que estás viendo. Revisa Logcat para el enlace del índice.
                    Log.w("UserProfileActivity", "Listen failed.", e)
                    Toast.makeText(this, "Error al cargar las publicaciones.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                postList.clear()
                for (document in snapshots!!) {
                    val post = document.toObject(Post::class.java).copy(id = document.id)
                    postList.add(post)
                }
                postsAdapter.notifyDataSetChanged()
            }
    }

    private fun toggleLike(post: Post) {
        val currentUser = auth.currentUser ?: return
        val postId = post.id ?: return // Salir si el id es nulo
        val postRef = db.collection("posts").document(postId)

        if (post.likedBy.contains(currentUser.uid)) {
            postRef.update("likedBy", FieldValue.arrayRemove(currentUser.uid))
        } else {
            postRef.update("likedBy", FieldValue.arrayUnion(currentUser.uid))
        }
        // No es necesario llamar a loadUserPosts(), addSnapshotListener se encarga de actualizar la UI
    }

    private fun showDeleteConfirmationDialog(post: Post) {
        val currentUser = auth.currentUser
        // Solo mostrar el diálogo si el post pertenece al usuario actual
        if (currentUser != null && currentUser.uid == post.userId) {
            AlertDialog.Builder(this)
                .setTitle("Confirmar borrado")
                .setMessage("¿Estás seguro de que quieres borrar este posteo?")
                .setPositiveButton("Borrar") { _, _ ->
                    deletePost(post)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } else {
            Toast.makeText(this, "No puedes borrar posteos de otros usuarios.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deletePost(post: Post) {
        val postId = post.id ?: run {
            Toast.makeText(this, "No se puede borrar el posteo (ID no encontrado).", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("posts").document(postId).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Posteo borrado exitosamente.", Toast.LENGTH_SHORT).show()
                // La UI se actualizará automáticamente gracias a addSnapshotListener
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al borrar el posteo: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.w("UserProfileActivity", "Error deleting document", e)
            }
    }
}
