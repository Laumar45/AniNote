package com.laumar.aninote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.laumar.aninote.data.AppPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ThemeUiState {
    data object Loading : ThemeUiState
    data class Success(val mode: String, val accent: String) : ThemeUiState
}

class ThemeViewModel(private val preferences: AppPreferences) : ViewModel() {

    val uiState: StateFlow<ThemeUiState> = combine(
        preferences.modeFlow,
        preferences.accentFlow
    ) { mode, accent ->
        ThemeUiState.Success(mode = mode, accent = accent)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeUiState.Loading
    )

    fun setMode(mode: String) = viewModelScope.launch { preferences.setMode(mode) }
    fun setAccent(accent: String) = viewModelScope.launch { preferences.setAccent(accent) }
}

class ThemeViewModelFactory(private val preferences: AppPreferences) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ThemeViewModel(preferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
