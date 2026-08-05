package com.laumar.anilista.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laumar.anilista.ui.components.AddEditDialog
import com.laumar.anilista.ui.components.AnimeCard
import com.laumar.anilista.ui.components.DeleteConfirmDialog
import com.laumar.anilista.ui.components.SortToggle
import com.laumar.anilista.ui.components.ThemeBottomSheet
import com.laumar.anilista.viewmodel.AnimeViewModel
import com.laumar.anilista.viewmodel.ThemeViewModel
import com.laumar.anilista.viewmodel.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeListScreen(
    viewModel: AnimeViewModel,
    themeViewModel: ThemeViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mode by themeViewModel.mode.collectAsStateWithLifecycle()
    val accent by themeViewModel.accent.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showThemeSheet by remember { mutableStateOf(false) }

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

    if (showThemeSheet) {
        ThemeBottomSheet(
            currentMode = mode,
            currentAccent = accent,
            onModeChange = themeViewModel::setMode,
            onAccentChange = themeViewModel::setAccent,
            onDismiss = { showThemeSheet = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi lista") },
                actions = {
                    SortToggle(
                        selected = uiState.sortOrder,
                        onSelectedChange = viewModel::onSortOrderChange
                    )
                    IconButton(onClick = { showThemeSheet = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Tema",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            if (uiState.animes.isEmpty() && uiState.query.isBlank()) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // --- Search bar ---
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar anime") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Buscar"
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Limpiar"
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // --- List ---
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.animes) { _, anime ->
                    AnimeCard(
                        anime = anime,
                        position = anime.id.toInt(),
                        onDelete = { viewModel.requestDelete(anime) },
                        modifier = Modifier.clickable { viewModel.openEditDialog(anime) }
                    )
                }
            }
        }
    }
}
