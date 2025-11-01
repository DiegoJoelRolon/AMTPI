package com.example.amtpi
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Pdi(
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Parcelable
