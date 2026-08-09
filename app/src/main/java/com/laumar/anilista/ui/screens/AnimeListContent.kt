package com.laumar.anilista.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.laumar.anilista.R
import com.laumar.anilista.data.AnimeEntity
import com.laumar.anilista.ui.components.AnimeCard
import com.laumar.anilista.ui.components.EmptyState
import com.laumar.anilista.viewmodel.SortOrder
import com.laumar.anilista.viewmodel.UiState

@Composable
fun AnimeListContent(
    uiState: UiState,
    contentPadding: PaddingValues,
    onQueryChange: (String) -> Unit,
    onEdit: (AnimeEntity) -> Unit,
    onDelete: (AnimeEntity) -> Unit,
    onSearchFocusChanged: (Boolean) -> Unit,
    onDismissSearchFocus: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        AnimeSearchField(
            query = uiState.query,
            onQueryChange = onQueryChange,
            onDismissSearchFocus = onDismissSearchFocus,
            onFocusChanged = onSearchFocusChanged
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .dismissSearchOnPointerDown(onDismissSearchFocus)
        ) {
            when {
                uiState.isInitialLoading -> InitialLoadingSkeleton()
                uiState.animes.isEmpty() -> EmptyState(
                    isEmptyList = uiState.query.isBlank(),
                    searchQuery = uiState.query,
                    onClearSearch = {
                        onQueryChange("")
                        onDismissSearchFocus()
                    }
                )
                else -> AnimeList(
                    animes = uiState.animes,
                    sortOrder = uiState.sortOrder,
                    onEdit = { anime ->
                        onDismissSearchFocus()
                        onEdit(anime)
                    },
                    onDelete = { anime ->
                        onDismissSearchFocus()
                        onDelete(anime)
                    },
                    onDismissSearchFocus = onDismissSearchFocus
                )
            }
        }
    }
}

@Composable
private fun AnimeSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismissSearchFocus: () -> Unit,
    onFocusChanged: (Boolean) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .onFocusChanged { onFocusChanged(it.isFocused) },
        placeholder = { Text(stringResource(R.string.search_placeholder)) },
        leadingIcon = { Icon(Icons.Default.Search, stringResource(R.string.search_content_desc)) },
        trailingIcon = {
            if (query.isNotEmpty()) IconButton(onClick = {
                onQueryChange("")
                onDismissSearchFocus()
            }) {
                Icon(Icons.Default.Close, stringResource(R.string.search_clear_content_desc))
            }
        },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDismissSearchFocus() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
private fun AnimeList(
    animes: List<AnimeEntity>,
    sortOrder: SortOrder,
    onEdit: (AnimeEntity) -> Unit,
    onDelete: (AnimeEntity) -> Unit,
    onDismissSearchFocus: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            onDismissSearchFocus()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .dismissSearchOnPointerDown(onDismissSearchFocus),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(animes, key = { _, anime -> anime.id }) { index, anime ->
            val position = if (sortOrder == SortOrder.ASC) index + 1 else animes.size - index
            AnimeCard(
                anime = anime,
                position = position,
                onDelete = { onDelete(anime) },
                modifier = Modifier.clickable {
                    onEdit(anime)
                }
            )
        }
    }
}

fun Modifier.dismissSearchOnPointerDown(onDismissSearchFocus: () -> Unit): Modifier =
    pointerInput(onDismissSearchFocus) {
        awaitEachGesture {
            awaitFirstDown(pass = PointerEventPass.Initial)
            onDismissSearchFocus()
        }
    }

@Composable
private fun InitialLoadingSkeleton() {
    val transition = rememberInfiniteTransition(label = "initialLoading")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeletonPulse"
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        items(12) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            ) {
                Box(Modifier.fillMaxWidth().height(56.dp))
            }
        }
    }
}
