package com.example.amtpi

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog // 1. Importa AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
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

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        postsRecyclerView = findViewById(R.id.posts_recyclerview)
        postsRecyclerView.layoutManager = LinearLayoutManager(this)

        // 2. Pasa el listener del clic largo al adaptador
        postsAdapter = PostsAdapter(postList) { post ->
            showDeleteConfirmationDialog(post)
        }
        postsRecyclerView.adapter = postsAdapter

        val btnCerrarSesion = findViewById<Button>(R.id.btn_cerrar_sesion)
        val btnNuevoPosteo = findViewById<Button>(R.id.btn_nuevo_posteo)

        btnNuevoPosteo.setOnClickListener {
            val intent = Intent(this, CreatePostActivity::class.java)
            startActivity(intent)
        }

        btnCerrarSesion.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        loadPosts()
    }

    // 3. Añade la función para mostrar el diálogo de confirmación
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

    // 4. Añade la función para borrar el post de Firestore
    private fun deletePost(post: Post) {
        val postId = post.id
        if (postId == null) {
            Toast.makeText(this, "No se puede borrar el posteo (ID no encontrado).", Toast.LENGTH_SHORT).show()
            return
        }

        // ¡Mucho más simple y directo!
        db.collection("posts").document(postId).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Posteo borrado exitosamente.", Toast.LENGTH_SHORT).show()
                // La lista se actualiza sola gracias al SnapshotListener, no necesitamos hacer nada más.
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
                    // postsRecyclerView.smoothScrollToPosition(0) // Comentado para evitar scroll al borrar
                }
            }
    }
}
