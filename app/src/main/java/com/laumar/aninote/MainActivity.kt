package com.laumar.aninote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laumar.aninote.data.AppDatabase
import com.laumar.aninote.repository.AnimeRepository
import com.laumar.aninote.ui.screens.AnimeListScreen
import com.laumar.aninote.ui.theme.AniNoteTheme
import com.laumar.aninote.viewmodel.AnimeViewModel
import com.laumar.aninote.viewmodel.AnimeViewModelFactory
import com.laumar.aninote.viewmodel.ThemeViewModel
import com.laumar.aninote.viewmodel.ThemeViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // La transición del splash screen debe inicializarse antes de super.onCreate
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = AnimeRepository(database.animeDao())

        val animeViewModel = ViewModelProvider(
            this,
            AnimeViewModelFactory(repository)
        )[AnimeViewModel::class.java]

        val themeViewModel = ViewModelProvider(
            this,
            ThemeViewModelFactory(applicationContext)
        )[ThemeViewModel::class.java]

        setContent {
            val mode by themeViewModel.mode.collectAsStateWithLifecycle()
            val accent by themeViewModel.accent.collectAsStateWithLifecycle()

            AniNoteTheme(mode = mode, accent = accent) {
                AnimeListScreen(
                    viewModel = animeViewModel,
                    themeViewModel = themeViewModel
                )
            }
        }
    }
}
