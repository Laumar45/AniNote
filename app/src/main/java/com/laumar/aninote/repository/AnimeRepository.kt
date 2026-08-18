package com.laumar.aninote.repository

import com.laumar.aninote.data.AnimeDao
import com.laumar.aninote.data.AnimeEntity
import kotlinx.coroutines.flow.Flow

class AnimeRepository(private val dao: AnimeDao) {

    fun getAllCanonical(): Flow<List<AnimeEntity>> = dao.getAllCanonical()

    suspend fun insert(anime: AnimeEntity): Long {
        return dao.insert(anime)
    }

    suspend fun update(anime: AnimeEntity) {
        dao.updateNameAndCount(anime.id, anime.nombre, anime.vecesVisto)
    }

    suspend fun delete(anime: AnimeEntity) {
        dao.delete(anime)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun findByNameCaseInsensitive(nombre: String): AnimeEntity? {
        return dao.findByNameCaseInsensitive(nombre)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    suspend fun insertAll(animes: List<AnimeEntity>) {
        dao.insertAll(animes)
    }

    suspend fun replaceAll(animes: List<AnimeEntity>) {
        dao.replaceAll(animes)
    }
}
