package com.laumar.aninote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laumar.aninote.data.AnimeEntity
import com.laumar.aninote.data.AppPreferences
import com.laumar.aninote.repository.AnimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AnimeViewModel(
    private val repository: AnimeRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _query.asStateFlow()

    val sortOrder: StateFlow<SortOrder> = preferences.sortOrderFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SortOrder.DESC)

    private val _activeFilter = MutableStateFlow(ListFilter.ALL)
    val activeFilter: StateFlow<ListFilter> = _activeFilter.asStateFlow()

    private val _dialog = MutableStateFlow(DialogState())
    val dialog: StateFlow<DialogState> = _dialog.asStateFlow()

    private val _pendingDeleteIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _pendingDeleteAnime = MutableStateFlow<AnimeEntity?>(null)
    val pendingDeleteAnime: StateFlow<AnimeEntity?> = _pendingDeleteAnime.asStateFlow()

    private var deleteJob: Job? = null

    @OptIn(FlowPreview::class)
    private val debouncedQueryFlow = _query
        .debounce(250)
        .distinctUntilChanged()

    val uiState: StateFlow<AnimeListUiState> = combine(
        repository.getAllCanonical(),
        debouncedQueryFlow,
        preferences.sortOrderFlow,
        _activeFilter,
        _pendingDeleteIds
    ) { entities, query, currentSortOrder, filter, pendingDeletes ->

        // 1. Excluir pending deletes (borrado con Undo)
        val visibleEntities = entities.filterNot { it.id in pendingDeletes }

        // 2. Asignar numeración canónica (1..N sobre el orden ascendente histórico)
        val canonicalList = visibleEntities.mapIndexed { index, entity ->
            AnimeUi(
                id = entity.id,
                numero = index + 1,
                nombre = entity.nombre,
                vecesVisto = entity.vecesVisto,
                createdAt = entity.createdAt
            )
        }

        // 3. Aplicar búsqueda por nombre (case-insensitive)
        val searchedList = if (query.isBlank()) canonicalList
        else canonicalList.filter { it.nombre.contains(query, ignoreCase = true) }

        // 4. Aplicar filtro secundario (Todos / vistos > 1)
        val filteredList = when (filter) {
            ListFilter.ALL -> searchedList
            ListFilter.REWATCHED -> searchedList.filter { it.vecesVisto > 1 }
        }

        // 5. Aplicar orden de vista (Recientes = DESC, Antiguos = ASC)
        val finalList = if (currentSortOrder == SortOrder.DESC) filteredList.asReversed() else filteredList

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

    // --- Search, Filter & Sort ---

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun onSortOrderChange(order: SortOrder) {
        viewModelScope.launch {
            preferences.setSortOrder(order)
        }
    }

    fun onFilterChange(filter: ListFilter) {
        _activeFilter.value = filter
    }

    // --- Dialog ---

    fun openAddDialog() {
        _dialog.value = DialogState(showDialog = true, nombre = "", vecesVisto = "1")
    }

    fun openEditDialog(anime: AnimeUi) {
        _dialog.value = DialogState(
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

    fun requestDelete(anime: AnimeUi) {
        _pendingDeleteAnime.value = AnimeEntity(
            id = anime.id,
            nombre = anime.nombre,
            vecesVisto = anime.vecesVisto,
            createdAt = anime.createdAt
        )
    }

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
        deleteJob?.cancel()
        _pendingDeleteIds.value += anime.id

        viewModelScope.launch {
            _events.send(UiEvent.ShowSnackbarWithUndo("${anime.nombre} eliminado", anime.id))
        }

        deleteJob = viewModelScope.launch {
            delay(4000)
            repository.deleteById(anime.id)
            _pendingDeleteIds.value -= anime.id
        }
    }

    fun undoDelete(animeId: Long) {
        deleteJob?.cancel()
        _pendingDeleteIds.value -= animeId
    }

    // --- Import/Export ---

    fun importAnimes(content: String, replace: Boolean) {
        val currentPending = _pendingDeleteAnime.value
        if (currentPending != null) {
            deleteJob?.cancel()
            viewModelScope.launch {
                repository.deleteById(currentPending.id)
                _pendingDeleteIds.value -= currentPending.id
                _pendingDeleteAnime.value = null
            }
        }
        importExportController.importAnimes(content, replace)
    }

    suspend fun getExportTxt(): String {
        val state = uiState.value
        val animes = if (state is AnimeListUiState.Success) state.animes else emptyList()
        val entities = animes.map {
            AnimeEntity(
                id = it.id,
                nombre = it.nombre,
                vecesVisto = it.vecesVisto,
                createdAt = it.createdAt
            )
        }
        return importExportController.getExportTxt(entities)
    }

    suspend fun getExportJson(): String {
        val state = uiState.value
        val animes = if (state is AnimeListUiState.Success) state.animes else emptyList()
        val entities = animes.map {
            AnimeEntity(
                id = it.id,
                nombre = it.nombre,
                vecesVisto = it.vecesVisto,
                createdAt = it.createdAt
            )
        }
        return importExportController.getExportJson(entities)
    }
}
