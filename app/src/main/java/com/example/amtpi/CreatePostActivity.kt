package com.example.amtpi

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream


class CreatePostActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val postImage = findViewById<ImageView>(R.id.create_post_image)
        val postContent = findViewById<EditText>(R.id.create_post_content)
        val btnCreatePost = findViewById<Button>(R.id.btn_create_post)


        imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }


        if (imageUri != null) {
            postImage.setImageURI(imageUri)
        } else {
            Toast.makeText(this, "No se proporcionó ninguna imagen.", Toast.LENGTH_LONG).show()
            finish()
            return
        }


        btnCreatePost.isEnabled = true

        btnCreatePost.setOnClickListener {
            val content = postContent.text.toString().trim()
            if (content.isNotEmpty()) {
                convertImageAndCreatePost(content)
            } else {
                Toast.makeText(this, "La publicación debe tener contenido.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun convertImageAndCreatePost(content: String) {
        val user = auth.currentUser

        if (user != null && imageUri != null) {
            try {
                val imageBase64 = uriToBase64(imageUri!!)
                if (imageBase64 != null) {
                    createPost(content, imageBase64)
                } else {
                    Toast.makeText(this, "Error al procesar la imagen.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("CreatePostActivity", "Error convirtiendo imagen a Base64", e)
            }
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(this.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun createPost(content: String, imageAsBase64: String) {
        val user = auth.currentUser ?: return

        val post = Post(
            content = content,
            username = user.displayName ?: "Anónimo",
            userId = user.uid,
            imageUrls = imageAsBase64
        )

        db.collection("posts")
            .add(post)
            .addOnSuccessListener {
                Toast.makeText(this, "Publicación creada con éxito", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al publicar: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("CreatePostActivity", "Error al crear la publicación", e)
            }
    }
}
