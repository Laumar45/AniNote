package com.laumar.aninote.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.laumar.aninote.R
import com.laumar.aninote.ui.components.SortToggle
import com.laumar.aninote.model.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeListTopBar(
    totalCount: Int,
    visibleCount: Int,
    isFilteredOrSearched: Boolean,
    sortOrder: SortOrder,
    onSortOrderChanged: (SortOrder) -> Unit,
    onOpenThemeSheet: () -> Unit,
    onImportRequested: () -> Unit,
    onExportTxtRequested: () -> Unit,
    onExportJsonRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by rememberSaveable { mutableStateOf(false) }

    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.title_my_list),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val subtitleText = when {
                        totalCount == 0 -> ""
                        isFilteredOrSearched -> stringResource(R.string.topbar_subtitle_filtered, visibleCount, totalCount)
                        totalCount == 1 -> stringResource(R.string.topbar_subtitle_total_one)
                        else -> stringResource(R.string.topbar_subtitle_total, totalCount)
                    }
                    if (subtitleText.isNotEmpty()) {
                        Crossfade(
                            targetState = subtitleText,
                            animationSpec = tween(durationMillis = 300),
                            label = "topBarSubtitleCrossfade"
                        ) { text ->
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                SortToggle(
                    selected = sortOrder,
                    onSelectedChange = onSortOrderChanged
                )
            }
        },
        actions = {
            IconButton(onClick = onOpenThemeSheet) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.settings_theme),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
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
                            onImportRequested()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_export_txt)) },
                        onClick = {
                            showMenu = false
                            onExportTxtRequested()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_export_json)) },
                        onClick = {
                            showMenu = false
                            onExportJsonRequested()
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}
