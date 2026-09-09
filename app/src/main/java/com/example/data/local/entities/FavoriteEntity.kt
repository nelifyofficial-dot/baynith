package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val movieId: String,
    val title: String,
    val posterPath: String,
    val backdropPath: String,
    val year: Int? = null,
    val rating: Double = 0.0,
    val genres: String = "",
    val savedTimestamp: Long = System.currentTimeMillis()
)
