package com.devrodrigo.movies

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "movies")
data class ModelMovies(
    @PrimaryKey val id: Int,
    val title: String,
    val vote_average: Double,
    val vote_count: Int,
    val status: String,
    val release_date: String?,
    val revenue: Long,
    val runtime: Int,
    val adult: Boolean,
    val backdrop_path: String?
)