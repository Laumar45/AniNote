package com.laumar.anilista.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "theme_preferences")

object ThemePreferences {

    private val MODE_KEY = stringPreferencesKey("mode")
    private val ACCENT_KEY = stringPreferencesKey("accent")

    val defaultMode = "system"
    val defaultAccent = "green"

    fun getMode(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[MODE_KEY] ?: defaultMode
        }
    }

    fun getAccent(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[ACCENT_KEY] ?: defaultAccent
        }
    }

    suspend fun setMode(context: Context, mode: String) {
        context.dataStore.edit { preferences ->
            preferences[MODE_KEY] = mode
        }
    }

    suspend fun setAccent(context: Context, accent: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCENT_KEY] = accent
        }
    }
}
