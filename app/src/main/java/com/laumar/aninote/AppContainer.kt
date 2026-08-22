package com.laumar.aninote

import android.content.Context
import com.laumar.aninote.data.AppDatabase
import com.laumar.aninote.data.AppPreferences
import com.laumar.aninote.repository.AnimeRepository

/**
 * Manual Dependency Injection container.
 * Centralizes the creation of all app-level dependencies (database, repository,
 * preferences) in a single place, making them easily accessible and testable
 * without requiring a DI framework.
 */
interface AppContainer {
    val animeRepository: AnimeRepository
    val appPreferences: AppPreferences
}

class DefaultAppContainer(context: Context) : AppContainer {

    private val database = AppDatabase.getInstance(context)

    override val animeRepository = AnimeRepository(database.animeDao())

    override val appPreferences = AppPreferences(context)
}
