package com.laumar.anilista.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.laumar.anilista.data.ThemePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext

    val mode: StateFlow<String> = ThemePreferences.getMode(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemePreferences.DEFAULT_MODE)

    val accent: StateFlow<String> = ThemePreferences.getAccent(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemePreferences.DEFAULT_ACCENT)

    fun setMode(mode: String) {
        viewModelScope.launch {
            ThemePreferences.setMode(appContext, mode)
        }
    }

    fun setAccent(accent: String) {
        viewModelScope.launch {
            ThemePreferences.setAccent(appContext, accent)
        }
    }
}

class ThemeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ThemeViewModel(context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
