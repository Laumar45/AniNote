package com.laumar.aninote.viewmodel

import com.laumar.aninote.data.AnimeEntity

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
