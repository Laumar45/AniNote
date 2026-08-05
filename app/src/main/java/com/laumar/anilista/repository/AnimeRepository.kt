package com.laumar.anilista.repository

import com.laumar.anilista.data.AnimeDao
import com.laumar.anilista.data.AnimeEntity
import kotlinx.coroutines.flow.Flow

class AnimeRepository(private val dao: AnimeDao) {

    val allAnimes: Flow<List<AnimeEntity>> = dao.getAll()

    suspend fun insert(anime: AnimeEntity) {
        dao.insert(anime)
    }

    suspend fun update(anime: AnimeEntity) {
        dao.update(anime)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun findByName(nombre: String): AnimeEntity? {
        return dao.findByName(nombre)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    suspend fun insertAll(animes: List<AnimeEntity>) {
        dao.insertAll(animes)
    }
}
