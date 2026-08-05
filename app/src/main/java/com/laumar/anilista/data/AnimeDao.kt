package com.laumar.anilista.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeDao {

    @Query("SELECT * FROM animes ORDER BY createdAt ASC")
    fun getAll(): Flow<List<AnimeEntity>>

    @Query("SELECT * FROM animes ORDER BY createdAt DESC")
    fun getAllDesc(): Flow<List<AnimeEntity>>

    @Query("SELECT * FROM animes WHERE LOWER(TRIM(nombre)) = LOWER(TRIM(:nombre)) LIMIT 1")
    suspend fun findByNameCaseInsensitive(nombre: String): AnimeEntity?

    @Insert
    suspend fun insert(anime: AnimeEntity)

    @Update
    suspend fun update(anime: AnimeEntity)

    @Delete
    suspend fun delete(anime: AnimeEntity)

    @Query("DELETE FROM animes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM animes")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(animes: List<AnimeEntity>)
}
