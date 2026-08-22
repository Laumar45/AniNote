package com.laumar.aninote.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Dedicated state holder for Add/Edit dialog form lifecycle and user input validation.
 * Keeps form state isolated from the main AnimeViewModel.
 */
class DialogStateHolder {

    private val _state = MutableStateFlow(DialogState())
    val state: StateFlow<DialogState> = _state.asStateFlow()

    fun openAdd() {
        _state.update { DialogState(showDialog = true, nombre = "", vecesVisto = "1") }
    }

    fun openEdit(anime: AnimeUi) {
        _state.update {
            DialogState(
                showDialog = true,
                editingAnimeId = anime.id,
                nombre = anime.nombre,
                vecesVisto = anime.vecesVisto.toString()
            )
        }
    }

    fun close() {
        _state.update { DialogState() }
    }

    fun onNombreChange(nombre: String) {
        _state.update { it.copy(nombre = nombre) }
    }

    fun onVecesVistoChange(vecesVisto: String) {
        if (vecesVisto.all { it.isDigit() }) {
            _state.update { it.copy(vecesVisto = vecesVisto) }
        }
    }

    fun consumeState(): DialogState {
        val current = _state.value
        _state.update { DialogState() }
        return current
    }
}
