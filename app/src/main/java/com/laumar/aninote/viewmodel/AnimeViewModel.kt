package com.laumar.aninote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laumar.aninote.data.AnimeEntity
import com.laumar.aninote.data.AppPreferences
import com.laumar.aninote.model.SortOrder
import com.laumar.aninote.repository.AnimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnimeViewModel(
    private val repository: AnimeRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _query.asStateFlow()

    val sortOrder: StateFlow<SortOrder> = preferences.sortOrderFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SortOrder.DESC)

    private val _dialog = MutableStateFlow(DialogState())
    val dialog: StateFlow<DialogState> = _dialog.asStateFlow()

    private val _pendingDeleteAnime = MutableStateFlow<AnimeEntity?>(null)
    val pendingDeleteAnime: StateFlow<AnimeEntity?> = _pendingDeleteAnime.asStateFlow()

    private val _highlightedAnimeId = MutableStateFlow<Long?>(null)
    val highlightedAnimeId: StateFlow<Long?> = _highlightedAnimeId.asStateFlow()

    private val recentlyDeletedAnimes = mutableMapOf<Long, AnimeEntity>()

    @OptIn(FlowPreview::class)
    private val debouncedQueryFlow = _query
        .debounce(250)
        .distinctUntilChanged()

    val uiState: StateFlow<AnimeListUiState> = combine(
        repository.getAllCanonical(),
        debouncedQueryFlow,
        preferences.sortOrderFlow
    ) { entities, query, currentSortOrder ->

        // 1. Asignar numeración canónica (1..N sobre el orden ascendente histórico)
        val canonicalList = entities.mapIndexed { index, entity ->
            AnimeUi(
                id = entity.id,
                numero = index + 1,
                nombre = entity.nombre,
                vecesVisto = entity.vecesVisto,
                createdAt = entity.createdAt
            )
        }

        // 2. Aplicar búsqueda por nombre (case-insensitive)
        val searchedList = if (query.isBlank()) canonicalList
        else canonicalList.filter { it.nombre.contains(query, ignoreCase = true) }

        // 3. Aplicar orden de vista (Recientes = DESC, Antiguos = ASC)
        val finalList = if (currentSortOrder == SortOrder.DESC) searchedList.asReversed() else searchedList

        AnimeListUiState.Success(
            animes = finalList,
            totalCount = canonicalList.size,
            visibleCount = finalList.size
        )
    }.flowOn(Dispatchers.Default)
     .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnimeListUiState.Loading
     )

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val importExportController = ImportExportController(
        repository = repository,
        scope = viewModelScope,
        emitEvent = _events::send
    )

    // --- Search & Sort ---

    fun onQueryChange(query: String) {
        _query.update { query }
    }

    fun onSortOrderChange(order: SortOrder) {
        viewModelScope.launch {
            preferences.setSortOrder(order)
        }
    }

    // --- Dialog ---

    fun openAddDialog() {
        _dialog.update { DialogState(showDialog = true, nombre = "", vecesVisto = "1") }
    }

    fun openEditDialog(anime: AnimeUi) {
        _dialog.update {
            DialogState(
                showDialog = true,
                editingAnime = AnimeEntity(
                    id = anime.id,
                    nombre = anime.nombre,
                    vecesVisto = anime.vecesVisto,
                    createdAt = anime.createdAt
                ),
                nombre = anime.nombre,
                vecesVisto = anime.vecesVisto.toString()
            )
        }
    }

    fun openEditDialog(anime: AnimeEntity) {
        _dialog.update {
            DialogState(
                showDialog = true,
                editingAnime = anime,
                nombre = anime.nombre,
                vecesVisto = anime.vecesVisto.toString()
            )
        }
    }

    fun closeDialog() {
        _dialog.update { DialogState() }
    }

    fun onDialogNombreChange(nombre: String) {
        _dialog.update { it.copy(nombre = nombre) }
    }

    fun onDialogVecesVistoChange(vecesVisto: String) {
        if (vecesVisto.all { it.isDigit() }) {
            _dialog.update { it.copy(vecesVisto = vecesVisto) }
        }
    }

    fun confirmDialog() {
        val state = _dialog.value
        val nombre = state.nombre.replace(Regex("[\\r\\n]+"), " ").trim()
        val vecesVisto = state.vecesVisto.toIntOrNull() ?: 1
        val editing = state.editingAnime

        if (nombre.isBlank()) return

        viewModelScope.launch {
            if (editing == null) {
                val newId = repository.insert(
                    AnimeEntity(nombre = nombre, vecesVisto = vecesVisto.coerceAtLeast(1))
                )
                preferences.setSortOrder(SortOrder.DESC)
                _highlightedAnimeId.update { newId }
                _events.send(UiEvent.ScrollToTop)
                _events.send(UiEvent.ShowSnackbar("Anime agregado"))

                viewModelScope.launch {
                    delay(1200)
                    _highlightedAnimeId.update { current ->
                        if (current == newId) null else current
                    }
                }
            } else {
                repository.update(
                    editing.copy(nombre = nombre, vecesVisto = vecesVisto.coerceAtLeast(1))
                )
                _events.send(UiEvent.ShowSnackbar("Anime actualizado"))
            }
            _dialog.update { DialogState() }
        }
    }

    // --- Delete confirmation & Undo ---

    fun requestDelete(anime: AnimeUi) {
        _pendingDeleteAnime.update {
            AnimeEntity(
                id = anime.id,
                nombre = anime.nombre,
                vecesVisto = anime.vecesVisto,
                createdAt = anime.createdAt
            )
        }
    }

    fun requestDelete(anime: AnimeEntity) {
        _pendingDeleteAnime.update { anime }
    }

    fun cancelDelete() {
        _pendingDeleteAnime.update { null }
    }

    fun confirmDelete() {
        val anime = _pendingDeleteAnime.value ?: return
        _pendingDeleteAnime.update { null }
        executeDelete(anime)
    }

    private fun executeDelete(anime: AnimeEntity) {
        recentlyDeletedAnimes[anime.id] = anime
        viewModelScope.launch {
            repository.deleteById(anime.id)
            _events.send(UiEvent.ShowSnackbarWithUndo("${anime.nombre} eliminado", anime.id))
        }
    }

    fun undoDelete(animeId: Long) {
        val animeToRestore = recentlyDeletedAnimes.remove(animeId)
        if (animeToRestore != null) {
            viewModelScope.launch {
                repository.insert(animeToRestore)
                _highlightedAnimeId.update { animeId }
                _events.send(UiEvent.ScrollToAnime(animeId))
                viewModelScope.launch {
                    delay(1200)
                    _highlightedAnimeId.update { current ->
                        if (current == animeId) null else current
                    }
                }
            }
        }
    }

    // --- Import/Export ---

    fun importAnimes(content: String, replace: Boolean) {
        recentlyDeletedAnimes.clear()
        importExportController.importAnimes(content, replace)
    }

    suspend fun getExportTxt(): String {
        val entities = repository.getAllCanonical().first()
        return importExportController.getExportTxt(entities)
    }

    suspend fun getExportJson(): String {
        val entities = repository.getAllCanonical().first()
        return importExportController.getExportJson(entities)
    }
}
