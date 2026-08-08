package com.laumar.anilista.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laumar.anilista.R
import com.laumar.anilista.ui.components.AddEditDialog
import com.laumar.anilista.ui.components.DeleteConfirmDialog
import com.laumar.anilista.ui.components.ImportConfirmDialog
import com.laumar.anilista.ui.components.ThemeBottomSheet
import com.laumar.anilista.viewmodel.AnimeViewModel
import com.laumar.anilista.viewmodel.ThemeViewModel
import com.laumar.anilista.viewmodel.UiEvent
import com.laumar.anilista.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeListScreen(
    viewModel: AnimeViewModel,
    themeViewModel: ThemeViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showThemeSheet by rememberSaveable { mutableStateOf(false) }
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    var pendingImportContent by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImportIsJson by rememberSaveable { mutableStateOf(false) }
    val fileActions = rememberAnimeListFileActions(
        viewModel = viewModel,
        onImportLoaded = { content, isJson ->
            pendingImportContent = content
            pendingImportIsJson = isJson
            showImportDialog = true
        }
    )

    AnimeListOverlays(
        viewModel = viewModel,
        themeViewModel = themeViewModel,
        uiState = uiState,
        showThemeSheet = showThemeSheet,
        pendingImportContent = pendingImportContent,
        pendingImportIsJson = pendingImportIsJson,
        onDismissThemeSheet = { showThemeSheet = false },
        onDismissImportDialog = {
            showImportDialog = false
            pendingImportContent = null
        },
        showImportDialog = showImportDialog
    )

    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.ShowSnackbarWithUndo -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(event.animeId)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AnimeListTopBar(
                sortOrder = uiState.sortOrder,
                showMenu = showMenu,
                onSortOrderChange = viewModel::onSortOrderChange,
                onShowThemeSheet = { showThemeSheet = true },
                onShowMenuChange = { showMenu = it },
                onImport = fileActions.launchImport,
                onExportTxt = fileActions.launchExportTxt,
                onExportJson = fileActions.launchExportJson
            )
        },
        floatingActionButton = {
            AnimeListFab(
                isEmptyList = !uiState.isInitialLoading && uiState.animes.isEmpty() && uiState.query.isBlank(),
                onClick = viewModel::openAddDialog
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        AnimeListContent(
            uiState = uiState,
            contentPadding = padding,
            onQueryChange = viewModel::onQueryChange,
            onEdit = viewModel::openEditDialog,
            onDelete = viewModel::requestDelete
        )
    }
}

@Composable
private fun AnimeListOverlays(
    viewModel: AnimeViewModel,
    themeViewModel: ThemeViewModel,
    uiState: UiState,
    showThemeSheet: Boolean,
    showImportDialog: Boolean,
    pendingImportContent: String?,
    pendingImportIsJson: Boolean,
    onDismissThemeSheet: () -> Unit,
    onDismissImportDialog: () -> Unit
) {
    if (showImportDialog && pendingImportContent != null) {
        ImportConfirmDialog(
            isJson = pendingImportIsJson,
            onReplace = {
                viewModel.importAnimes(pendingImportContent, replace = true)
                onDismissImportDialog()
            },
            onCombine = {
                viewModel.importAnimes(pendingImportContent, replace = false)
                onDismissImportDialog()
            },
            onDismiss = onDismissImportDialog
        )
    }

    if (uiState.dialog.showDialog) {
        val title = if (uiState.dialog.editingAnime == null) {
            stringResource(R.string.dialog_add_title)
        } else {
            stringResource(R.string.dialog_edit_title)
        }
        AddEditDialog(
            title = title,
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
        val mode by themeViewModel.mode.collectAsStateWithLifecycle()
        val accent by themeViewModel.accent.collectAsStateWithLifecycle()
        ThemeBottomSheet(
            currentMode = mode,
            currentAccent = accent,
            onModeChange = themeViewModel::setMode,
            onAccentChange = themeViewModel::setAccent,
            onDismiss = onDismissThemeSheet
        )
    }
}

@Composable
private fun AnimeListFab(isEmptyList: Boolean, onClick: () -> Unit) {
    if (isEmptyList) {
        androidx.compose.material3.ExtendedFloatingActionButton(
            text = { Text(stringResource(R.string.fab_add_anime)) },
            icon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.fab_add)) },
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    } else {
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.fab_add))
        }
    }
}
