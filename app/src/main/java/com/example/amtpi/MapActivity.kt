package com.example.amtpi

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Timer
import kotlin.concurrent.timer

class MapActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private lateinit var db: FirebaseFirestore


    private lateinit var btnNewPost: Button
    private val pdiList = mutableListOf<Pdi>()
    private var proximityCheckTimer: Timer? = null
    private var imageUri: Uri? = null
    private val MAX_DISTANCE_METERS = 50.0

    private val cameraResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            imageUri?.let { uri ->
                val intent = Intent(this, CreatePostActivity::class.java).apply {
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            }
        } else {
            Toast.makeText(this, "Captura de foto cancelada.", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(this, "Permiso de cámara denegado.", Toast.LENGTH_SHORT).show()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, androidx.preference.PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(R.layout.activity_map)

        db = FirebaseFirestore.getInstance()
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)


        btnNewPost = findViewById(R.id.btn_new_post_map)
        btnNewPost.visibility = View.GONE
        btnNewPost.setOnClickListener {
            checkCameraPermissionAndOpenCamera()
        }


        setupUserLocationOverlay()
        loadAndDrawAllPdis()
        centerMapBasedOnIntent()
    }


    private fun centerMapBasedOnIntent() {
        val pdi: Pdi? = intent.getParcelableExtra("PDI_EXTRA")
        val mapController = map.controller

        if (pdi != null) {
            mapController.setZoom(18.5)
            val pdiPoint = GeoPoint(pdi.latitude, pdi.longitude)
            mapController.setCenter(pdiPoint)
        } else {
            mapController.setZoom(18.0)
            locationOverlay.runOnFirstFix {
                runOnUiThread {
                    locationOverlay.myLocation?.let { mapController.animateTo(it) }
                }
            }
        }
    }

    private fun loadAndDrawAllPdis() {
        db.collection("pdilist").get().addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                Log.d("MapActivity", "No se encontraron PDI.")
                return@addOnSuccessListener
            }
            pdiList.clear()
            for (doc in documents) {
                val pdi = doc.toObject(Pdi::class.java)
                pdiList.add(pdi)
                val pdiPoint = GeoPoint(pdi.latitude, pdi.longitude)
                addPdiMarker(pdiPoint, pdi.name, pdi.description)
            }
            map.invalidate()
        }.addOnFailureListener { e ->
            Log.w("MapActivity", "Error al cargar los PDI", e)
        }
    }

    private fun addPdiMarker(geoPoint: GeoPoint, title: String, description: String) {
        val pdiMarker = Marker(map)
        pdiMarker.position = geoPoint
        pdiMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        pdiMarker.title = title
        pdiMarker.snippet = description
        map.overlays.add(pdiMarker)
    }

    private fun setupUserLocationOverlay() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationProvider = GpsMyLocationProvider(this)
            locationOverlay = MyLocationNewOverlay(locationProvider, map)

            val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.my_location_icon)


            val desiredWidth = 100
            val desiredHeight = 100


            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, desiredWidth, desiredHeight, true)


            locationOverlay.setPersonIcon(scaledBitmap)


            locationOverlay.setDirectionArrow(scaledBitmap, scaledBitmap)
            locationOverlay.enableMyLocation()
            map.overlays.add(locationOverlay)
        }
    }


    private fun startProximityChecker() {
        proximityCheckTimer?.cancel()
        proximityCheckTimer = timer(period = 5000) {
            runOnUiThread {
                checkProximityToPdis()
            }
        }
    }


    private fun stopProximityChecker() {
        proximityCheckTimer?.cancel()
        proximityCheckTimer = null
    }


    private fun checkProximityToPdis() {
        val userLocation = locationOverlay.myLocation
        if (userLocation == null || pdiList.isEmpty()) {
            btnNewPost.visibility = View.GONE
            return
        }

        var isNearAnyPdi = false
        val currentUserLocation = Location("user").apply {
            latitude = userLocation.latitude
            longitude = userLocation.longitude
        }

        for (pdi in pdiList) {
            val pdiLocation = Location("pdi").apply {
                latitude = pdi.latitude
                longitude = pdi.longitude
            }
            if (currentUserLocation.distanceTo(pdiLocation) < MAX_DISTANCE_METERS) {
                isNearAnyPdi = true
                break
            }
        }


        btnNewPost.visibility = if (isNearAnyPdi) View.VISIBLE else View.GONE
    }


    private fun checkCameraPermissionAndOpenCamera() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }


    private fun openCamera() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "Post_Image_${System.currentTimeMillis()}")
            put(MediaStore.Images.Media.DESCRIPTION, "Imagen para nuevo post")
        }
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        }
        cameraResultLauncher.launch(intent)
    }



    override fun onResume() {
        super.onResume()
        map.onResume()
        startProximityChecker()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        stopProximityChecker()
    }
}

