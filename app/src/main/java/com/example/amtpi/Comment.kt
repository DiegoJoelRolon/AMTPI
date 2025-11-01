package com.example.amtpi

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import kotlinx.parcelize.Parcelize

@Parcelize
data class Comment(
    @get:Exclude var id: String? = null,
    val commentText: String = "",
    val username: String = "",
    val userId: String = "",
    @ServerTimestamp
    val timestamp: Date? = null,
    val likedBy: List<String> = emptyList()
) : Parcelable
