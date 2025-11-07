package com.example.amtpi

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore


class PdiActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var pdiAdapter: PdiAdapter
    private val pdiList = mutableListOf<Pdi>()

    private lateinit var db: FirebaseFirestore


    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pdi)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.pdi_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        pdiAdapter = PdiAdapter(pdiList)
        recyclerView.adapter = pdiAdapter

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        requestLocationAndLoadPdis()

        //addHardcodedPdisToFirestore()
    }


    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            loadPdisWithLocation()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado. No se puede ordenar por cercanía.", Toast.LENGTH_LONG).show()
            loadPdiFromFirestore(null)
        }
    }



    private fun requestLocationAndLoadPdis() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        } else {
            loadPdisWithLocation()
        }
    }

    private fun loadPdisWithLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    loadPdiFromFirestore(location)
                } else {
                    Toast.makeText(this, "No se pudo obtener la ubicación actual. Mostrando lista por defecto.", Toast.LENGTH_LONG).show()
                    loadPdiFromFirestore(null)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener ubicación.", Toast.LENGTH_SHORT).show()
                loadPdiFromFirestore(null)
            }
    }


    private fun loadPdiFromFirestore(userLocation: Location?) {
        db.collection("pdilist")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("PdiActivity", "Error al escuchar cambios.", e)
                    Toast.makeText(this, "Error al cargar los PDI.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                pdiList.clear()
                val tempList = mutableListOf<Pdi>()
                for (doc in snapshots!!) {
                    val pdi = doc.toObject(Pdi::class.java)
                    tempList.add(pdi)
                }

                if (userLocation != null) {
                    tempList.sortWith(compareBy { pdi ->
                        val pdiLocation = Location("").apply {
                            latitude = pdi.latitude
                            longitude = pdi.longitude
                        }
                        userLocation.distanceTo(pdiLocation)
                    })

                } else {
                    tempList.sortBy { it.name }
                }

                pdiList.addAll(tempList)
                pdiAdapter.notifyDataSetChanged()
            }
    }


    private fun addHardcodedPdisToFirestore() {
        val hardcodedPdiList = listOf(
            Pdi("Punto Control", "Punto de control", "https://static.vecteezy.com/system/resources/previews/027/989/305/non_2x/placeholder-icon-in-trendy-flat-style-isolated-on-white-background-placeholder-silhouette-symbol-for-your-website-design-logo-app-ui-illustration-eps10-free-vector.jpg", -34.840864490159525, -58.28114842783235)
        )

        val pdiCollection = db.collection("pdilist")
        for (pdi in hardcodedPdiList) {
            pdiCollection.add(pdi)
                .addOnSuccessListener { documentReference ->
                    Log.d("PdiActivity", "PDI añadido con ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.w("PdiActivity", "Error al añadir PDI", e)
                }
        }
    }
}
