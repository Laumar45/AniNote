package com.laumar.aninote.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.laumar.aninote.R
import com.laumar.aninote.data.AnimeEntity
import com.laumar.aninote.ui.components.AddEditDialog
import com.laumar.aninote.ui.components.DeleteConfirmDialog
import com.laumar.aninote.ui.components.ImportConfirmDialog
import com.laumar.aninote.ui.components.ThemeBottomSheet
import com.laumar.aninote.viewmodel.AnimeListUiState
import com.laumar.aninote.viewmodel.AnimeViewModel
import com.laumar.aninote.viewmodel.DialogState
import com.laumar.aninote.viewmodel.ThemeUiState
import com.laumar.aninote.viewmodel.ThemeViewModel
import com.laumar.aninote.viewmodel.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeListScreen(
    viewModel: AnimeViewModel,
    themeViewModel: ThemeViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val dialogState by viewModel.dialog.collectAsStateWithLifecycle()
    val pendingDeleteAnime by viewModel.pendingDeleteAnime.collectAsStateWithLifecycle()
    val highlightedAnimeId by viewModel.highlightedAnimeId.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val dismissSearchFocus = rememberDismissSearchFocus()
    val snackbarHostState = remember { SnackbarHostState() }
    var isSearchFocused by remember { mutableStateOf(false) }
    var showThemeSheet by rememberSaveable { mutableStateOf(false) }
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

    BackHandler(enabled = isSearchFocused) {
        dismissSearchFocus()
    }

    AnimeListOverlays(
        viewModel = viewModel,
        themeViewModel = themeViewModel,
        dialogState = dialogState,
        pendingDeleteAnime = pendingDeleteAnime,
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
    var snackbarJob: Job? = null
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    snackbarJob = launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(event.message)
                    }
                }
                is UiEvent.ShowSnackbarWithUndo -> {
                    snackbarJob?.cancel()
                    snackbarJob = launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val result = snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = undoLabel,
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoDelete(event.animeId)
                        }
                    }
                }
                is UiEvent.ScrollToTop -> {
                    launch {
                        listState.animateScrollToItem(0)
                    }
                }
                is UiEvent.ScrollToAnime -> {
                    launch {
                        val targetIndex = withTimeoutOrNull(1500) {
                            viewModel.uiState
                                .map { state ->
                                    (state as? AnimeListUiState.Success)
                                        ?.animes
                                        ?.indexOfFirst { it.id == event.animeId }
                                        ?: -1
                                }
                                .filter { it >= 0 }
                                .first()
                        }
                        if (targetIndex != null) {
                            listState.animateScrollToItem(targetIndex)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(sortOrder) {
        listState.animateScrollToItem(0)
    }

    val totalCount = (uiState as? AnimeListUiState.Success)?.totalCount ?: 0
    val visibleCount = (uiState as? AnimeListUiState.Success)?.visibleCount ?: 0
    val isFilteredOrSearched = searchQuery.isNotBlank() || (totalCount != visibleCount)

    Scaffold(
        topBar = {
            AnimeListTopBar(
                totalCount = totalCount,
                visibleCount = visibleCount,
                isFilteredOrSearched = isFilteredOrSearched,
                sortOrder = sortOrder,
                onSortOrderChanged = {
                    dismissSearchFocus()
                    viewModel.onSortOrderChange(it)
                },
                onOpenThemeSheet = {
                    dismissSearchFocus()
                    showThemeSheet = true
                },
                onImportRequested = {
                    dismissSearchFocus()
                    fileActions.launchImport()
                },
                onExportTxtRequested = {
                    dismissSearchFocus()
                    fileActions.launchExportTxt()
                },
                onExportJsonRequested = {
                    dismissSearchFocus()
                    fileActions.launchExportJson()
                },
                modifier = Modifier.dismissSearchOnPointerDown(dismissSearchFocus)
            )
        },
        floatingActionButton = {
            val isFabVisible = !isSearchFocused && searchQuery.isBlank()
            val isEmptyList = (uiState as? AnimeListUiState.Success)?.animes?.isEmpty() == true && searchQuery.isBlank()

            AnimatedVisibility(
                visible = isFabVisible,
                enter = fadeIn(tween(200)) + scaleIn(tween(200)),
                exit = fadeOut(tween(200)) + scaleOut(tween(200))
            ) {
                AnimeListFab(
                    isEmptyList = isEmptyList,
                    onClick = {
                        dismissSearchFocus()
                        viewModel.openAddDialog()
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        AnimeListContent(
            searchQuery = searchQuery,
            uiState = uiState,
            highlightedAnimeId = highlightedAnimeId,
            listState = listState,
            contentPadding = padding,
            onQueryChange = viewModel::onQueryChange,
            onEdit = viewModel::openEditDialog,
            onChipClick = viewModel::openEditDialog,
            onDelete = viewModel::requestDelete,
            onSearchFocusChanged = { isSearchFocused = it },
            onDismissSearchFocus = { if (isSearchFocused) dismissSearchFocus() }
        )
    }
}

@Composable
private fun rememberDismissSearchFocus(): () -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return remember(focusManager, keyboardController) {
        {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }
}

@Composable
private fun AnimeListOverlays(
    viewModel: AnimeViewModel,
    themeViewModel: ThemeViewModel,
    dialogState: DialogState,
    pendingDeleteAnime: AnimeEntity?,
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

    if (dialogState.showDialog) {
        val title = if (dialogState.editingAnime == null) {
            stringResource(R.string.dialog_add_title)
        } else {
            stringResource(R.string.dialog_edit_title)
        }
        AddEditDialog(
            title = title,
            nombre = dialogState.nombre,
            vecesVisto = dialogState.vecesVisto,
            onNombreChange = viewModel::onDialogNombreChange,
            onVecesVistoChange = viewModel::onDialogVecesVistoChange,
            onConfirm = viewModel::confirmDialog,
            onDismiss = viewModel::closeDialog
        )
    }

    pendingDeleteAnime?.let { anime ->
        DeleteConfirmDialog(
            animeName = anime.nombre,
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::cancelDelete
        )
    }

    if (showThemeSheet) {
        val themeState by themeViewModel.uiState.collectAsStateWithLifecycle()
        if (themeState is ThemeUiState.Success) {
            val successState = themeState as ThemeUiState.Success
            ThemeBottomSheet(
                currentMode = successState.mode,
                currentAccent = successState.accent,
                onModeChange = themeViewModel::setMode,
                onAccentChange = themeViewModel::setAccent,
                onDismiss = onDismissThemeSheet
            )
        }
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
