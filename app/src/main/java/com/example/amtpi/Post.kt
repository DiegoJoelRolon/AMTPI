package com.example.amtpi

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import kotlinx.parcelize.Parcelize

@Parcelize
data class Post(
    @get:Exclude val id: String? = null,
    val imageUrls: String = "",
    val content: String = "",
    val username: String = "",
    val userId: String = "",
    @ServerTimestamp
    val timestamp: Date? = null,
    val likedBy: List<String> = emptyList(),
    val likedByCount: Int = 0
) : Parcelable
