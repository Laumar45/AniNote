package com.laumar.anilista.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.laumar.anilista.R
import com.laumar.anilista.ui.components.SortToggle
import com.laumar.anilista.viewmodel.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeListTopBar(
    sortOrder: SortOrder,
    showMenu: Boolean,
    onSortOrderChange: (SortOrder) -> Unit,
    onShowThemeSheet: () -> Unit,
    onShowMenuChange: (Boolean) -> Unit,
    onImport: () -> Unit,
    onExportTxt: () -> Unit,
    onExportJson: () -> Unit
) {
    TopAppBar(
        title = { Text(stringResource(R.string.title_my_list)) },
        actions = {
            SortToggle(selected = sortOrder, onSelectedChange = onSortOrderChange)
            IconButton(onClick = onShowThemeSheet) {
                Icon(Icons.Default.Settings, stringResource(R.string.settings_theme), tint = MaterialTheme.colorScheme.onBackground)
            }
            IconButton(onClick = { onShowMenuChange(true) }) {
                Icon(Icons.Default.MoreVert, stringResource(R.string.menu_content_desc), tint = MaterialTheme.colorScheme.onBackground)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { onShowMenuChange(false) }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.menu_import)) }, onClick = {
                    onShowMenuChange(false)
                    onImport()
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.menu_export_txt)) }, onClick = {
                    onShowMenuChange(false)
                    onExportTxt()
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.menu_export_json)) }, onClick = {
                    onShowMenuChange(false)
                    onExportJson()
                })
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}
