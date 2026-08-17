package com.laumar.aninote.viewmodel

import com.laumar.aninote.data.AnimeEntity
import com.laumar.aninote.repository.AnimeRepository
import com.laumar.aninote.utils.parseTxtFile
import com.laumar.aninote.utils.parseJson
import com.laumar.aninote.utils.formatTxtExport
import com.laumar.aninote.utils.serializeJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImportExportController(
    private val repository: AnimeRepository,
    private val scope: CoroutineScope,
    private val emitEvent: suspend (UiEvent) -> Unit
) {

    /**
     * Import animes from content string.
     * Format is auto-detected: JSON if content starts with '{', TXT otherwise.
     * @param content Raw file content
     * @param replace true to replace all existing animes, false to combine (skip duplicates)
     */
    fun importAnimes(content: String, replace: Boolean) {
        scope.launch {
            try {
                // Parse + entity building off main thread
                val importData = withContext(Dispatchers.Default) {
                    var ignoredCount = 0
                    val isJson = content.trimStart().startsWith("{")
                    val animesToImport: List<Pair<String, Int>> = if (isJson) {
                        parseJson(content)
                    } else {
                        val result = parseTxtFile(content)
                        ignoredCount = result.ignoredCount
                        result.animes.map { it to 1 }
                    }
                    Triple(animesToImport, ignoredCount, replace)
                }

                val (animesToImport, ignoredCount, replaceMode) = importData

                if (animesToImport.isEmpty()) {
                    emitEvent(UiEvent.ShowSnackbar("No se encontraron animes en el archivo"))
                    return@launch
                }

                if (replaceMode) {
                    repository.deleteAll()
                    val base = System.currentTimeMillis()
                    val entities = mutableListOf<AnimeEntity>()
                    animesToImport.forEachIndexed { index, (nombre, vecesVisto) ->
                        entities.add(
                            AnimeEntity(
                                nombre = nombre,
                                vecesVisto = vecesVisto.coerceAtLeast(1),
                                createdAt = base + index
                            )
                        )
                    }
                    repository.insertAll(entities)
                    val ignored = if (ignoredCount > 0) " ($ignoredCount líneas ignoradas por formato inválido)" else ""
                    emitEvent(UiEvent.ShowSnackbar("Importaste ${animesToImport.size} animes${ignored} (lista reemplazada)"))
                } else {
                    // Combine: skip duplicates
                    var imported = 0
                    var duplicates = 0
                    for ((nombre, vecesVisto) in animesToImport) {
                        val existing = repository.findByNameCaseInsensitive(nombre)
                        if (existing == null) {
                            repository.insert(
                                AnimeEntity(nombre = nombre, vecesVisto = vecesVisto.coerceAtLeast(1))
                            )
                            imported++
                        } else {
                            duplicates++
                        }
                    }
                    val parts = mutableListOf("Importaste $imported animes")
                    if (ignoredCount > 0) parts.add("$ignoredCount líneas ignoradas por formato inválido")
                    if (duplicates > 0) parts.add("$duplicates duplicados omitidos")
                    val msg = if (parts.size > 1) {
                        "${parts[0]} (${parts.drop(1).joinToString(", ")})"
                    } else {
                        parts[0]
                    }
                    emitEvent(UiEvent.ShowSnackbar(msg))
                }
            } catch (e: Exception) {
                val msg = when {
                    e is kotlinx.serialization.SerializationException -> "Archivo JSON inválido"
                    e is IllegalArgumentException -> e.message ?: "Error al importar"
                    e.message?.contains("JSON") == true -> "Archivo JSON inválido"
                    else -> "Error al importar archivo"
                }
                emitEvent(UiEvent.ShowSnackbar(msg))
            }
        }
    }

    /**
     * Get export content for TXT format.
     * @param animes List of anime entities to export
     */
    fun getExportTxt(animes: List<AnimeEntity>): String {
        val data = animes.map { it.nombre to it.vecesVisto }
        return formatTxtExport(data)
    }

    /**
     * Get export content for JSON format.
     * @param animes List of anime entities to export
     */
    fun getExportJson(animes: List<AnimeEntity>): String {
        return serializeJson(animes)
    }
}
