package com.laumar.aninote.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.laumar.aninote.model.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "app_preferences")

class AppPreferences(private val context: Context) {

    companion object {
        val MODE_KEY = stringPreferencesKey("mode")
        val ACCENT_KEY = stringPreferencesKey("accent")
        val SORT_ORDER_KEY = stringPreferencesKey("sort")

        const val DEFAULT_MODE = "system"
        const val DEFAULT_ACCENT = "green"
        val DEFAULT_SORT_ORDER = SortOrder.DESC
    }

    val modeFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[MODE_KEY] ?: DEFAULT_MODE
        }

    val accentFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[ACCENT_KEY] ?: DEFAULT_ACCENT
        }

    val sortOrderFlow: Flow<SortOrder> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            when (preferences[SORT_ORDER_KEY]) {
                "asc" -> SortOrder.ASC
                "desc" -> SortOrder.DESC
                else -> DEFAULT_SORT_ORDER
            }
        }

    suspend fun setMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[MODE_KEY] = mode
        }
    }

    suspend fun setAccent(accent: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCENT_KEY] = accent
        }
    }

    suspend fun setSortOrder(sortOrder: SortOrder) {
        context.dataStore.edit { preferences ->
            preferences[SORT_ORDER_KEY] = when (sortOrder) {
                SortOrder.ASC -> "asc"
                SortOrder.DESC -> "desc"
            }
        }
    }
}
