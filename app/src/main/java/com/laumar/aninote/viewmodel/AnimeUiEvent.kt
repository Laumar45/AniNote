package com.laumar.aninote.viewmodel

sealed interface UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent
    data class ShowSnackbarWithUndo(val message: String, val animeId: Long) : UiEvent
    data object ScrollToTop : UiEvent
    data class ScrollToAnime(val animeId: Long) : UiEvent
}
