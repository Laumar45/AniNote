package com.laumar.aninote.viewmodel

import com.laumar.aninote.data.AnimeEntity
import com.laumar.aninote.repository.AnimeRepository
import com.laumar.aninote.utils.ImportResult
import com.laumar.aninote.utils.formatTxtExport
import com.laumar.aninote.utils.parseJson
import com.laumar.aninote.utils.parseTxtFile
import com.laumar.aninote.utils.serializeJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImportExportController(
    private val repository: AnimeRepository,
    private val scope: CoroutineScope,
    private val emitEvent: suspend (UiEvent) -> Unit
) {

    /**
     * Import animes from content string.
     * Format is auto-detected: JSON if content trimmed starts with '{', TXT otherwise.
     * @param content Raw file content
     * @param replace true to replace all existing animes, false to combine (skip duplicates)
     */
    fun importAnimes(content: String, replace: Boolean) {
        scope.launch {
            try {
                // 1. Parsing & deserialization executed strictly on Dispatchers.Default
                val (animesToImport, ignoredCount) = withContext(Dispatchers.Default) {
                    val trimmed = content.trim()
                    val isJson = trimmed.startsWith("{") && trimmed.endsWith("}")
                    if (isJson) {
                        val parsed = parseJson(content)
                        parsed to 0
                    } else {
                        val result = parseTxtFile(content)
                        result.animes.map { it to 1 } to result.ignoredCount
                    }
                }

                if (animesToImport.isEmpty()) {
                    emitEvent(UiEvent.ShowSnackbar("No se encontraron animes en el archivo"))
                    return@launch
                }

                // 2. Database transaction executed strictly on Dispatchers.IO
                val importResult = withContext(Dispatchers.IO) {
                    val baseTimestamp = System.currentTimeMillis()

                    if (replace) {
                        val entities = animesToImport.mapIndexed { index, (nombre, vecesVisto) ->
                            AnimeEntity(
                                nombre = nombre,
                                vecesVisto = vecesVisto.coerceAtLeast(1),
                                createdAt = baseTimestamp + index
                            )
                        }
                        repository.replaceAll(entities)
                        ImportResult(
                            importedCount = entities.size,
                            skippedDuplicates = 0,
                            invalidLines = ignoredCount
                        )
                    } else {
                        // Combine mode: Case-insensitive in-memory deduplication
                        val existingNames = repository.getAllCanonical().first()
                            .map { it.nombre.trim().lowercase() }
                            .toSet()

                        val newEntities = mutableListOf<AnimeEntity>()
                        var duplicates = 0

                        animesToImport.forEachIndexed { index, (nombre, vecesVisto) ->
                            val normalized = nombre.trim().lowercase()
                            if (normalized in existingNames || newEntities.any { it.nombre.trim().lowercase() == normalized }) {
                                duplicates++
                            } else {
                                newEntities.add(
                                    AnimeEntity(
                                        nombre = nombre,
                                        vecesVisto = vecesVisto.coerceAtLeast(1),
                                        createdAt = baseTimestamp + index
                                    )
                                )
                            }
                        }

                        if (newEntities.isNotEmpty()) {
                            repository.insertAll(newEntities)
                        }

                        ImportResult(
                            importedCount = newEntities.size,
                            skippedDuplicates = duplicates,
                            invalidLines = ignoredCount
                        )
                    }
                }

                // 3. User feedback message
                val parts = mutableListOf("Importaste ${importResult.importedCount} animes")
                if (importResult.invalidLines > 0) {
                    parts.add("${importResult.invalidLines} líneas ignoradas")
                }
                if (importResult.skippedDuplicates > 0) {
                    parts.add("${importResult.skippedDuplicates} duplicados omitidos")
                }
                if (replace) {
                    parts.add("lista reemplazada")
                }

                val message = if (parts.size > 1) {
                    "${parts[0]} (${parts.drop(1).joinToString(", ")})"
                } else {
                    parts[0]
                }
                emitEvent(UiEvent.ShowSnackbar(message))

            } catch (e: Exception) {
                val msg = when {
                    e is kotlinx.serialization.SerializationException -> "Archivo JSON inválido o malformado"
                    e is IllegalArgumentException -> e.message ?: "Versión o formato de archivo no soportado"
                    else -> "Error al procesar el archivo de importación"
                }
                emitEvent(UiEvent.ShowSnackbar(msg))
            }
        }
    }

    /**
     * Get export content for TXT format on Dispatchers.Default.
     * @param animes List of anime entities to export
     */
    suspend fun getExportTxt(animes: List<AnimeEntity>): String = withContext(Dispatchers.Default) {
        val data = animes.map { it.nombre to it.vecesVisto }
        formatTxtExport(data)
    }

    /**
     * Get export content for JSON format on Dispatchers.Default.
     * @param animes List of anime entities to export
     */
    suspend fun getExportJson(animes: List<AnimeEntity>): String = withContext(Dispatchers.Default) {
        serializeJson(animes)
    }
}
