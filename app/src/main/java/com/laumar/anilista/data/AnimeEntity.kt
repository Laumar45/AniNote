package com.laumar.anilista.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "animes")
data class AnimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val vecesVisto: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)
