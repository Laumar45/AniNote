package com.laumar.anilista.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.laumar.anilista.R
import androidx.compose.material.icons.automirrored.filled.MenuBook

/**
 * Empty states for the anime list.
 *
 * Two variants:
 * 1. Empty list (no search active): prompts user to add first anime
 * 2. No search results: shows query and clear button
 */
@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    isEmptyList: Boolean,
    searchQuery: String = "",
    onClearSearch: () -> Unit = {}
) {
    if (isEmptyList) {
        EmptyListState(modifier = modifier)
    } else {
        NoResultsState(
            searchQuery = searchQuery,
            onClearSearch = onClearSearch,
            modifier = modifier
        )
    }
}

@Composable
private fun EmptyListState(modifier: Modifier = Modifier) {
    val listDesc = stringResource(R.string.empty_list_desc)

    Column(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = listDesc },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.empty_list_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.empty_list_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
private fun NoResultsState(
    searchQuery: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val noResultsDesc = stringResource(R.string.empty_no_results_desc, searchQuery)

    Column(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = noResultsDesc },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.empty_no_results_text, searchQuery),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onClearSearch) {
            Text(stringResource(R.string.empty_clear_search))
        }
    }
}
