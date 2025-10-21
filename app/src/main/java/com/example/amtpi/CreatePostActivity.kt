package com.example.amtpi

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreatePostActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_post)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Obtener elementos de la UI
        val postContent = findViewById<EditText>(R.id.create_post_content)
        val btnCreatePost = findViewById<Button>(R.id.btn_create_post)

        btnCreatePost.setOnClickListener {
            val content = postContent.text.toString()
            if (content.isNotEmpty()) {
                createPost(content)
            } else {
                Toast.makeText(this, "La publicación no puede estar vacía", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createPost(content: String) {
        val user = auth.currentUser
        if (user != null) {
            val post = Post(
                content = content,
                username = user.displayName ?: "Anónimo",
                userId = user.uid
            )

            db.collection("posts")
                .add(post)
                .addOnFailureListener { e ->Toast.makeText(this, "Error al publicar: ${e.message}", Toast.LENGTH_SHORT).show()

                    Log.e("CreatePostActivity", "Error al crear la publicación", e)
                }
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
