package com.laumar.anilista.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laumar.anilista.ui.components.AddEditDialog
import com.laumar.anilista.ui.components.AnimeCard
import com.laumar.anilista.ui.components.DeleteConfirmDialog
import com.laumar.anilista.viewmodel.AnimeViewModel
import com.laumar.anilista.viewmodel.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeListScreen(viewModel: AnimeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.ShowSnackbarWithUndo -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "Deshacer",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete(event.animeId)
                    }
                }
            }
        }
    }

    if (uiState.dialog.showDialog) {
        val dialogTitle = if (uiState.dialog.editingAnime == null) "Nuevo anime" else "Editar"
        AddEditDialog(
            title = dialogTitle,
            nombre = uiState.dialog.nombre,
            vecesVisto = uiState.dialog.vecesVisto,
            onNombreChange = viewModel::onDialogNombreChange,
            onVecesVistoChange = viewModel::onDialogVecesVistoChange,
            onConfirm = viewModel::confirmDialog,
            onDismiss = viewModel::closeDialog
        )
    }

    uiState.pendingDeleteAnime?.let { anime ->
        DeleteConfirmDialog(
            animeName = anime.nombre,
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::cancelDelete
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi lista") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            if (uiState.animes.isEmpty()) {
                ExtendedFloatingActionButton(
                    text = { Text("Agregar anime") },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Agregar") },
                    onClick = viewModel::openAddDialog,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                FloatingActionButton(
                    onClick = viewModel::openAddDialog,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(uiState.animes) { index, anime ->
                AnimeCard(
                    anime = anime,
                    position = index + 1,
                    onDelete = { viewModel.requestDelete(anime) },
                    modifier = Modifier.clickable { viewModel.openEditDialog(anime) }
                )
            }
        }
    }
}
