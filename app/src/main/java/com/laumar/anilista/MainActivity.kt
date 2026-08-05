package com.laumar.anilista

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.laumar.anilista.data.AppDatabase
import com.laumar.anilista.repository.AnimeRepository
import com.laumar.anilista.ui.screens.AnimeListScreen
import com.laumar.anilista.ui.theme.AniListaTheme
import com.laumar.anilista.viewmodel.AnimeViewModel
import com.laumar.anilista.viewmodel.AnimeViewModelFactory
import com.laumar.anilista.viewmodel.ThemeViewModel
import com.laumar.anilista.viewmodel.ThemeViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
            AniListaTheme {
                AnimeListScreen(viewModel = animeViewModel)
            }
        }
    }
}
