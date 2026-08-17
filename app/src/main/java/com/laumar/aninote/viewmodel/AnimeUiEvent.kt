package com.laumar.aninote.viewmodel

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data class ShowSnackbarWithUndo(val message: String, val animeId: Long) : UiEvent()
}
