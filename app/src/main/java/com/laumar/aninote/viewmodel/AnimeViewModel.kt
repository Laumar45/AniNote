package com.laumar.aninote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laumar.aninote.data.AnimeEntity
import com.laumar.aninote.repository.AnimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

class AnimeViewModel(private val repository: AnimeRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.ASC)
    private val _dialog = MutableStateFlow(DialogState())
    private val _pendingDeleteIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _pendingDeleteAnime = MutableStateFlow<AnimeEntity?>(null)
    private var deleteJob: Job? = null

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val searchQueryDebounced = _query
        .debounce(250)
        .distinctUntilChanged()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val dataState: StateFlow<DataState> = combine(
        _sortOrder.flatMapLatest { order ->
            when (order) {
                SortOrder.ASC -> repository.allAnimes
                SortOrder.DESC -> repository.allAnimesDesc
            }
        },
        searchQueryDebounced,
        _sortOrder
    ) { animes, query, sortOrder ->
        DataState(
            animes = animes.filter { it.nombre.contains(query, ignoreCase = true) },
            query = query,
            sortOrder = sortOrder,
            isInitialLoading = false
        )
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DataState())

    val uiState: StateFlow<UiState> = combine(
        dataState,
        _query,
        _dialog,
        _pendingDeleteIds,
        _pendingDeleteAnime
    ) { data, query, dialog, pendingDeleteIds, pendingDeleteAnime ->
        UiState(
            animes = data.animes.filter { it.id !in pendingDeleteIds },
            query = query,
            sortOrder = data.sortOrder,
            dialog = dialog,
            pendingDeleteIds = pendingDeleteIds,
            pendingDeleteAnime = pendingDeleteAnime,
            isInitialLoading = data.isInitialLoading
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val importExportController = ImportExportController(
        repository = repository,
        scope = viewModelScope,
        emitEvent = _events::send
    )

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

    fun importAnimes(content: String, replace: Boolean) {
        importExportController.importAnimes(content, replace)
    }

    fun getExportTxt(): String {
        return importExportController.getExportTxt(uiState.value.animes)
    }

    fun getExportJson(): String {
        return importExportController.getExportJson(uiState.value.animes)
    }
}
