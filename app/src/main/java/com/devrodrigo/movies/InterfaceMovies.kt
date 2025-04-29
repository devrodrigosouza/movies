package com.devrodrigo.movies

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface InterfaceMovies {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<ModelMovies>)

    @Query("SELECT * FROM movies")
    suspend fun getAll(): List<ModelMovies>

    @Query("SELECT * FROM movies WHERE id = :movieId LIMIT 1")
    suspend fun getMovieById(movieId: Int): ModelMovies

    @Query("DELETE FROM movies")
    fun deleteAll()
}
