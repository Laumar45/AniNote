package com.laumar.aninote.viewmodel

import com.laumar.aninote.utils.ImportResult

sealed interface UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent
    data class ShowSnackbarWithUndo(val message: String, val animeId: Long) : UiEvent
    data class ShowImportSuccess(val result: ImportResult, val isReplaced: Boolean) : UiEvent
    data class ShowImportError(val error: ImportError) : UiEvent
    data object ScrollToTop : UiEvent
    data class ScrollToAnime(val animeId: Long) : UiEvent
}

enum class ImportError {
    EMPTY_FILE,
    INVALID_JSON,
    UNSUPPORTED_VERSION,
    GENERIC
}

