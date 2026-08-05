package com.laumar.anilista.utils

import com.laumar.anilista.data.AnimeEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JSON schema for anime list export/import.
 * Version 1: { "version": 1, "animes": [{ "nombre": "...", "vecesVisto": 1 }] }
 */
@Serializable
data class AnimeJson(
    val nombre: String,
    val vecesVisto: Int
)

@Serializable
data class AnimeListJson(
    val version: Int = 1,
    val animes: List<AnimeJson> = emptyList()
)

/**
 * Reusable Json configuration.
 * - prettyPrint: human-readable output
 * - ignoreUnknownKeys: forward compatibility with future schema versions
 */
val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

/**
 * Parse a JSON string into a list of anime (nombre, vecesVisto).
 *
 * @param content Raw JSON string
 * @return List of pairs (nombre, vecesVisto)
 * @throws IllegalArgumentException if version is not supported
 * @throws kotlinx.serialization.SerializationException if JSON is malformed
 */
fun parseJson(content: String): List<Pair<String, Int>> {
    val parsed = json.decodeFromString<AnimeListJson>(content)
    require(parsed.version == 1) { "Versión de schema no soportada: ${parsed.version}" }
    return parsed.animes.map { it.nombre to it.vecesVisto }
}

/**
 * Serialize a list of AnimeEntity to JSON string.
 *
 * @param animes List of anime entities from database
 * @return Pretty-printed JSON string
 */
fun serializeJson(animes: List<AnimeEntity>): String {
    val data = AnimeListJson(
        version = 1,
        animes = animes.map { AnimeJson(it.nombre, it.vecesVisto) }
    )
    return json.encodeToString(AnimeListJson.serializer(), data)
}
