package com.laumar.anilista.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.laumar.anilista.data.AnimeEntity
import com.laumar.anilista.repository.AnimeRepository
import com.laumar.anilista.utils.parseTxtFile
import com.laumar.anilista.utils.parseJson
import com.laumar.anilista.utils.formatTxtExport
import com.laumar.anilista.utils.serializeJson
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class DialogState(
    val showDialog: Boolean = false,
    val editingAnime: AnimeEntity? = null,
    val nombre: String = "",
    val vecesVisto: String = "1"
)

enum class SortOrder { ASC, DESC }

data class DataState(
    val animes: List<AnimeEntity> = emptyList(),
    val query: String = "",
    val sortOrder: SortOrder = SortOrder.ASC,
    val isInitialLoading: Boolean = true
)

data class UiState(
    val animes: List<AnimeEntity> = emptyList(),
    val query: String = "",
    val sortOrder: SortOrder = SortOrder.ASC,
    val dialog: DialogState = DialogState(),
    val pendingDeleteIds: Set<Long> = emptySet(),
    val pendingDeleteAnime: AnimeEntity? = null,
    val isInitialLoading: Boolean = true
)

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data class ShowSnackbarWithUndo(val message: String, val animeId: Long) : UiEvent()
}

class AnimeViewModel(private val repository: AnimeRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.ASC)
    private val _dialog = MutableStateFlow(DialogState())
    private val _pendingDeleteIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _pendingDeleteAnime = MutableStateFlow<AnimeEntity?>(null)
    private var deleteJob: Job? = null

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val dataState: StateFlow<DataState> = combine(
        _sortOrder.flatMapLatest { order ->
            when (order) {
                SortOrder.ASC -> repository.allAnimes
                SortOrder.DESC -> repository.allAnimesDesc
            }
        },
        _query,
        _sortOrder
    ) { animes, query, sortOrder ->
        DataState(
            animes = animes.filter { it.nombre.contains(query, ignoreCase = true) },
            query = query,
            sortOrder = sortOrder,
            isInitialLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DataState())

    val uiState: StateFlow<UiState> = combine(
        dataState,
        _dialog,
        _pendingDeleteIds,
        _pendingDeleteAnime
    ) { data, dialog, pendingDeleteIds, pendingDeleteAnime ->
        UiState(
            animes = data.animes.filter { it.id !in pendingDeleteIds },
            query = data.query,
            sortOrder = data.sortOrder,
            dialog = dialog,
            pendingDeleteIds = pendingDeleteIds,
            pendingDeleteAnime = pendingDeleteAnime,
            isInitialLoading = data.isInitialLoading
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // --- Search & Sort ---

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun onSortOrderChange(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
    }

    // --- Dialog ---

    fun openAddDialog() {
        _dialog.value = DialogState(showDialog = true, nombre = "", vecesVisto = "1")
    }

    fun openEditDialog(anime: AnimeEntity) {
        _dialog.value = DialogState(
            showDialog = true,
            editingAnime = anime,
            nombre = anime.nombre,
            vecesVisto = anime.vecesVisto.toString()
        )
    }

    fun closeDialog() {
        _dialog.value = DialogState()
    }

    fun onDialogNombreChange(nombre: String) {
        _dialog.value = _dialog.value.copy(nombre = nombre)
    }

    fun onDialogVecesVistoChange(vecesVisto: String) {
        if (vecesVisto.all { it.isDigit() }) {
            _dialog.value = _dialog.value.copy(vecesVisto = vecesVisto)
        }
    }

    fun confirmDialog() {
        val state = _dialog.value
        // Strip newlines and collapse whitespace — names shouldn't contain line breaks
        val nombre = state.nombre.replace(Regex("[\\r\\n]+"), " ").trim()
        val vecesVisto = state.vecesVisto.toIntOrNull() ?: 1
        val editing = state.editingAnime

        if (nombre.isBlank()) return

        viewModelScope.launch {
            if (editing == null) {
                repository.insert(
                    AnimeEntity(nombre = nombre, vecesVisto = vecesVisto.coerceAtLeast(1))
                )
                _events.send(UiEvent.ShowSnackbar("Anime agregado"))
            } else {
                repository.update(
                    editing.copy(nombre = nombre, vecesVisto = vecesVisto.coerceAtLeast(1))
                )
                _events.send(UiEvent.ShowSnackbar("Anime actualizado"))
            }
            _dialog.value = DialogState()
        }
    }

    // --- Delete confirmation ---

    fun requestDelete(anime: AnimeEntity) {
        _pendingDeleteAnime.value = anime
    }

    fun cancelDelete() {
        _pendingDeleteAnime.value = null
    }

    fun confirmDelete() {
        val anime = _pendingDeleteAnime.value ?: return
        _pendingDeleteAnime.value = null
        executeDelete(anime)
    }

    private fun executeDelete(anime: AnimeEntity) {
        // Cancelar undo anterior si existe
        deleteJob?.cancel()

        // Marcar como pending delete (se oculta de la lista)
        _pendingDeleteIds.value += anime.id

        // Enviar evento con snackbar + undo (send es suspend, va en coroutine)
        viewModelScope.launch {
            _events.send(UiEvent.ShowSnackbarWithUndo("${anime.nombre} eliminado", anime.id))
        }

        // Lanzar coroutine con delay de 4 segundos
        deleteJob = viewModelScope.launch {
            delay(4000)
            // Si expira el delay, borrar definitivamente
            repository.deleteById(anime.id)
            _pendingDeleteIds.value -= anime.id
        }
    }

    fun undoDelete(animeId: Long) {
        // Cancelar el job (no borrar de Room)
        deleteJob?.cancel()

        // Quitar del pending delete (vuelve a aparecer en la lista)
        _pendingDeleteIds.value -= animeId
    }

    // --- Import/Export ---

    /**
     * Import animes from content string.
     * Format is auto-detected: JSON if content starts with '{', TXT otherwise.
     * @param content Raw file content
     * @param replace true to replace all existing animes, false to combine (skip duplicates)
     */
    fun importAnimes(content: String, replace: Boolean) {
        viewModelScope.launch {
            try {
                var ignoredCount = 0
                val isJson = content.trimStart().startsWith("{")
                val animesToImport: List<Pair<String, Int>> = if (isJson) {
                    parseJson(content)
                } else {
                    val result = parseTxtFile(content)
                    ignoredCount = result.ignoredCount
                    result.animes.map { it to 1 }
                }

                if (animesToImport.isEmpty()) {
                    _events.send(UiEvent.ShowSnackbar("No se encontraron animes en el archivo"))
                    return@launch
                }

                if (replace) {
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
                    _events.send(UiEvent.ShowSnackbar("Importaste ${animesToImport.size} animes${ignored} (lista reemplazada)"))
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
                    _events.send(UiEvent.ShowSnackbar(msg))
                }
            } catch (e: Exception) {
                val msg = when {
                    e is kotlinx.serialization.SerializationException -> "Archivo JSON inválido"
                    e is IllegalArgumentException -> e.message ?: "Error al importar"
                    e.message?.contains("JSON") == true -> "Archivo JSON inválido"
                    else -> "Error al importar archivo"
                }
                _events.send(UiEvent.ShowSnackbar(msg))
            }
        }
    }

    /**
     * Get export content for TXT format.
     */
    fun getExportTxt(): String {
        val animes = uiState.value.animes.map { it.nombre to it.vecesVisto }
        return formatTxtExport(animes)
    }

    /**
     * Get export content for JSON format.
     */
    fun getExportJson(): String {
        return serializeJson(uiState.value.animes)
    }
}

class AnimeViewModelFactory(private val repository: AnimeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnimeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnimeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
