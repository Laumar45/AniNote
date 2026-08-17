package com.laumar.aninote.data

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

    const val DEFAULT_MODE = "system"
    const val DEFAULT_ACCENT = "green"

    fun getMode(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[MODE_KEY] ?: DEFAULT_MODE
        }
    }

    fun getAccent(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[ACCENT_KEY] ?: DEFAULT_ACCENT
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
