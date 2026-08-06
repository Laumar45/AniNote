package com.laumar.anilista.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laumar.anilista.R
import com.laumar.anilista.ui.components.AddEditDialog
import com.laumar.anilista.ui.components.AnimeCard
import com.laumar.anilista.ui.components.DeleteConfirmDialog
import com.laumar.anilista.ui.components.EmptyState
import com.laumar.anilista.ui.components.ImportConfirmDialog
import com.laumar.anilista.ui.components.SortToggle
import com.laumar.anilista.ui.components.ThemeBottomSheet
import com.laumar.anilista.viewmodel.AnimeViewModel
import com.laumar.anilista.viewmodel.ThemeViewModel
import com.laumar.anilista.viewmodel.UiEvent
import com.laumar.anilista.viewmodel.SortOrder

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
    val context = LocalContext.current

    // SAF launcher for import (OpenDocument)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                reader.readText()
            }
            if (content != null) {
                val isJson = it.toString().endsWith(".json", ignoreCase = true)
                pendingImportContent = content
                pendingImportIsJson = isJson
                showImportDialog = true
            }
        }
    }

    // SAF launcher for export TXT
    val exportTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer ->
                writer.write(viewModel.getExportTxt())
            }
            viewModel.events.let { /* Event will be sent by ViewModel */ }
        }
    }

    // SAF launcher for export JSON
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer ->
                writer.write(viewModel.getExportJson())
            }
        }
    }

    // Import confirmation dialog
    if (showImportDialog && pendingImportContent != null) {
        ImportConfirmDialog(
            isJson = pendingImportIsJson,
            onReplace = {
                viewModel.importAnimes(pendingImportContent!!, replace = true)
                showImportDialog = false
                pendingImportContent = null
            },
            onCombine = {
                viewModel.importAnimes(pendingImportContent!!, replace = false)
                showImportDialog = false
                pendingImportContent = null
            },
            onDismiss = {
                showImportDialog = false
                pendingImportContent = null
            }
        )
    }

    val undoLabel = stringResource(R.string.action_undo)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.ShowSnackbarWithUndo -> {
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
        }
    }

    if (uiState.dialog.showDialog) {
        val dialogTitle = if (uiState.dialog.editingAnime == null) {
            stringResource(R.string.dialog_add_title)
        } else {
            stringResource(R.string.dialog_edit_title)
        }
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
        val mode by themeViewModel.mode.collectAsStateWithLifecycle()
        val accent by themeViewModel.accent.collectAsStateWithLifecycle()
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
                title = { Text(stringResource(R.string.title_my_list)) },
                actions = {
                    SortToggle(
                        selected = uiState.sortOrder,
                        onSelectedChange = viewModel::onSortOrderChange
                    )
                    IconButton(onClick = { showThemeSheet = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_theme),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.menu_content_desc),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_import)) },
                            onClick = {
                                showMenu = false
                                importLauncher.launch(arrayOf("text/plain", "application/json"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_export_txt)) },
                            onClick = {
                                showMenu = false
                                exportTxtLauncher.launch("anime_list.txt")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_export_json)) },
                            onClick = {
                                showMenu = false
                                exportJsonLauncher.launch("anime_list.json")
                            }
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
                    text = { Text(stringResource(R.string.fab_add_anime)) },
                    icon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.fab_add)) },
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
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.fab_add))
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
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_content_desc)
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.search_clear_content_desc)
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

            // --- Content ---
            if (uiState.animes.isEmpty()) {
                EmptyState(
                    isEmptyList = uiState.query.isBlank(),
                    searchQuery = uiState.query,
                    onClearSearch = { viewModel.onQueryChange("") }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                itemsIndexed(uiState.animes, key = { _, anime -> anime.id }) { index, anime ->
                    val position = when (uiState.sortOrder) {
                        SortOrder.ASC -> index + 1
                        SortOrder.DESC -> uiState.animes.size - index
                    }
                    AnimeCard(
                        anime = anime,
                        position = position,
                        onDelete = { viewModel.requestDelete(anime) },
                        modifier = Modifier.clickable { viewModel.openEditDialog(anime) }
                    )
                }
                }
            }
        }
    }
}
