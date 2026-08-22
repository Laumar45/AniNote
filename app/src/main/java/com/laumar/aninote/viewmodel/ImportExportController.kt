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
                        result.animes to result.ignoredCount
                    }
                }

                if (animesToImport.isEmpty()) {
                    emitEvent(UiEvent.ShowImportError(ImportError.EMPTY_FILE))
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
                        // Combine mode: Case-insensitive SQL deduplication with O(1) HashSet lookup
                        val seenNames = repository.getAllNamesNormalized().toHashSet()

                        val newEntities = mutableListOf<AnimeEntity>()
                        var duplicates = 0

                        animesToImport.forEachIndexed { index, (nombre, vecesVisto) ->
                            val normalized = nombre.trim().lowercase()
                            if (normalized in seenNames) {
                                duplicates++
                            } else {
                                seenNames.add(normalized)
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

                // 3. User feedback event
                emitEvent(UiEvent.ShowImportSuccess(importResult, replace))

            } catch (e: Exception) {
                val error = when (e) {
                    is kotlinx.serialization.SerializationException -> ImportError.INVALID_JSON
                    is IllegalArgumentException -> ImportError.UNSUPPORTED_VERSION
                    else -> ImportError.GENERIC
                }
                emitEvent(UiEvent.ShowImportError(error))
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
