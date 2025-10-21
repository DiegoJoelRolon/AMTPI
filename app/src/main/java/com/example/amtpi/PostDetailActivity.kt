package com.example.amtpi

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class PostDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        // Obtener referencias a las vistas del layout
        val usernameTextView = findViewById<TextView>(R.id.detail_post_username)
        val contentTextView = findViewById<TextView>(R.id.detail_post_content)
        val timestampTextView = findViewById<TextView>(R.id.detail_post_timestamp)
        val imageView = findViewById<ImageView>(R.id.detail_post_image) // Referencia al ImageView

        // Obtener el objeto Post pasado desde el adapter
        val post: Post? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("POST_DETAIL", Post::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("POST_DETAIL")
        }

        // Si el post no es nulo, rellenar las vistas con sus datos
        if (post != null) {
            usernameTextView.text = post.username
            contentTextView.text = post.content

            // Formatear y mostrar el timestamp
            post.timestamp?.let {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                timestampTextView.text = sdf.format(it)
            }

            // Cargar la imagen usando Glide si la URL no está vacía
            if (post.imageUrls.isNotEmpty()) {
                imageView.visibility = View.VISIBLE // Hacer visible el ImageView
                Glide.with(this)
                    .load(post.imageUrls)
                    .into(imageView)
            } else {
                // Si no hay imagen, ocultar el ImageView
                imageView.visibility = View.GONE
            }
        }
    }
}
