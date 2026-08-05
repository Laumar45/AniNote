package com.laumar.anilista.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun AniListaTheme(
    mode: String = "system",
    accent: String = "green",
    content: @Composable () -> Unit
) {
    val isDark = when (mode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val schemes = AccentColorSchemes[accent] ?: AccentColorSchemes["green"]!!
    val colorScheme = if (isDark) schemes.second else schemes.first

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
