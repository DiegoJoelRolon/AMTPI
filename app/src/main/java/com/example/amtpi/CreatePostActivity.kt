package com.example.amtpi

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class CreatePostActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    // private lateinit var storage: FirebaseStorage // Ya no se necesita

    private var imageUri: Uri? = null
    private lateinit var postImage: ImageView

    private val cameraResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            imageUri?.let {
                // La imagen ya debería estar en la Uri, así que la mostramos.
                postImage.setImageURI(it)
            }
        } else {
            // Si el usuario cancela la cámara, imageUri puede quedar invalidado.
            imageUri = null
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        // storage = FirebaseStorage.getInstance() // Ya no se necesita

        postImage = findViewById(R.id.create_post_image)
        val postContent = findViewById<EditText>(R.id.create_post_content)
        val btnCreatePost = findViewById<Button>(R.id.btn_create_post)
        val btnOpenCamera = findViewById<Button>(R.id.btn_open_camera)

        btnOpenCamera.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    openCamera()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }

        btnCreatePost.setOnClickListener {
            val content = postContent.text.toString()
            if (imageUri != null && content.isNotEmpty()) {
                // Se llama a la nueva función que convierte a Base64
                convertImageAndCreatePost(content)
            } else {
                Toast.makeText(this, "La publicación debe tener contenido y una imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openCamera() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "New Picture")
            put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        }
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        }
        cameraResultLauncher.launch(intent)
    }

    // --- INICIO DE CÓDIGO MODIFICADO ---

    // Función que reemplaza a "uploadImageAndCreatePost"
    private fun convertImageAndCreatePost(content: String) {
        val user = auth.currentUser
        if (user != null && imageUri != null) {
            try {
                // Convierte la Uri de la imagen a una cadena Base64
                val imageBase64 = uriToBase64(imageUri!!)

                if (imageBase64 != null) {
                    // Llama directamente a createPost con la cadena Base64
                    createPost(content, imageBase64)
                } else {
                    Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("CreatePostActivity", "Error convirtiendo imagen a Base64", e)
            }
        }
    }

    // Nueva función para convertir una Uri a una cadena Base64
    private fun uriToBase64(uri: Uri): String? {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(this.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }

            val outputStream = ByteArrayOutputStream()
            // Comprime la imagen a JPEG con calidad 50 para reducir significativamente su tamaño
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)

            // Codifica el array de bytes a una cadena Base64
            Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- FIN DE CÓDIGO MODIFICADO ---

    private fun createPost(content: String, imageAsBase64: String) {
        val user = auth.currentUser
        if (user != null) {
            val post = Post(
                content = content,
                username = user.displayName ?: "Anónimo",
                userId = user.uid,
                imageUrls = imageAsBase64
            )

            db.collection("posts")
                .add(post)
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al publicar: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("CreatePostActivity", "Error al crear la publicación", e)
                }
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
