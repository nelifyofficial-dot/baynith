package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "my_list")
data class MyListEntity(
    @PrimaryKey val movieId: String,
    val title: String = "",
    val posterPath: String = "",
    val backdropPath: String = "",
    val year: Int? = null,
    val rating: Double = 0.0,
    val genres: String = "",
    val isEmbed: Boolean = false,
    val savedTimestamp: Long = System.currentTimeMillis()
)
