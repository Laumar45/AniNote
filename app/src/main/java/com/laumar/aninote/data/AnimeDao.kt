package com.laumar.aninote.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeDao {

    @Query("SELECT * FROM animes ORDER BY createdAt ASC, id ASC")
    fun getAllCanonical(): Flow<List<AnimeEntity>>

    @Query("SELECT LOWER(TRIM(nombre)) FROM animes")
    suspend fun getAllNamesNormalized(): List<String>

    @Query("SELECT * FROM animes WHERE LOWER(TRIM(nombre)) = LOWER(TRIM(:nombre)) LIMIT 1")
    suspend fun findByNameCaseInsensitive(nombre: String): AnimeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(anime: AnimeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(animes: List<AnimeEntity>)

    @Update
    suspend fun update(anime: AnimeEntity)

    @Query("UPDATE animes SET nombre = :nombre, vecesVisto = :vecesVisto WHERE id = :id")
    suspend fun updateNameAndCount(id: Long, nombre: String, vecesVisto: Int)

    @Delete
    suspend fun delete(anime: AnimeEntity)

    @Query("DELETE FROM animes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM animes")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(animes: List<AnimeEntity>) {
        deleteAll()
        insertAll(animes)
    }
}
