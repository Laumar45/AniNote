package com.laumar.aninote.viewmodel

import androidx.compose.runtime.Immutable
import com.laumar.aninote.data.AnimeEntity

enum class SortOrder { DESC, ASC }

enum class ListFilter { ALL, REWATCHED }

@Immutable
data class AnimeUi(
    val id: Long,
    val numero: Int,
    val nombre: String,
    val vecesVisto: Int,
    val createdAt: Long = 0L
)

sealed interface AnimeListUiState {
    data object Loading : AnimeListUiState
    data class Success(
        val animes: List<AnimeUi>,
        val totalCount: Int,
        val visibleCount: Int
    ) : AnimeListUiState
}

data class DialogState(
    val showDialog: Boolean = false,
    val editingAnime: AnimeEntity? = null,
    val nombre: String = "",
    val vecesVisto: String = "1"
)
