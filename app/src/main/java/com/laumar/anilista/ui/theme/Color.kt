package com.laumar.anilista.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Capa 1 — Modo claro
private val LightBackground = Color(0xFFFAFAF9)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFF0F0EE)
private val LightOnBackground = Color(0xFF1A1A1A)
private val LightOnSurface = Color(0xFF1A1A1A)
private val LightOnSurfaceVariant = Color(0xFF6B6B6B)
private val LightOutline = Color(0xFFE0E0DD)

// Capa 1 — Modo oscuro
private val DarkBackground = Color(0xFF121212)
private val DarkSurface = Color(0xFF1E1E1E)
private val DarkSurfaceVariant = Color(0xFF262626)
private val DarkOnBackground = Color(0xFFEDEDED)
private val DarkOnSurface = Color(0xFFEDEDED)
private val DarkOnSurfaceVariant = Color(0xFF9E9E9E)
private val DarkOutline = Color(0xFF333333)

// Capa 2 — Colores de acento por semilla
private val GreenPrimary = Color(0xFF4CAF50)
private val OrangePrimary = Color(0xFFFF7A45)
private val BluePrimary = Color(0xFF4B7BE5)
private val PurplePrimary = Color(0xFF8B6FE0)

/**
 * Genera un ColorScheme completo a partir de un color primario.
 * Evita definir 8 esquemas a mano — un solo color semilla produce
 * primary, onPrimary, primaryContainer, onPrimaryContainer y secondary armónicos.
 */
private fun lightScheme(seed: Color) = lightColorScheme(
    primary = seed,
    onPrimary = Color.White,
    primaryContainer = seed.copy(alpha = 0.12f),
    onPrimaryContainer = seed.copy(alpha = 0.8f),
    secondary = seed.copy(alpha = 0.7f),
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

private fun darkScheme(seed: Color) = darkColorScheme(
    primary = seed,
    onPrimary = Color.Black,
    primaryContainer = seed.copy(alpha = 0.2f),
    onPrimaryContainer = seed.copy(alpha = 0.9f),
    secondary = seed.copy(alpha = 0.6f),
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

// Mapa público: nombre del acento → pair(light, dark)
val AccentColorSchemes = mapOf(
    "green" to Pair(lightScheme(GreenPrimary), darkScheme(GreenPrimary)),
    "orange" to Pair(lightScheme(OrangePrimary), darkScheme(OrangePrimary)),
    "blue" to Pair(lightScheme(BluePrimary), darkScheme(BluePrimary)),
    "purple" to Pair(lightScheme(PurplePrimary), darkScheme(PurplePrimary))
)

// Colores visuales para los círculos del selector de acento
val AccentCircleColors = mapOf(
    "green" to GreenPrimary,
    "orange" to OrangePrimary,
    "blue" to BluePrimary,
    "purple" to PurplePrimary
)
