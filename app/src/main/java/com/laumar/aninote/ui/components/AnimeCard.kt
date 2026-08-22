package com.laumar.aninote.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.laumar.aninote.R
import com.laumar.aninote.viewmodel.AnimeUi

@Composable
fun AnimeCard(
    anime: AnimeUi,
    isHighlighted: Boolean = false,
    onDelete: () -> Unit,
    onCopy: (String) -> Unit,
    onSearchWeb: (String) -> Unit,
    onChipClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {

    val chipDesc = if (anime.vecesVisto > 1) {
        stringResource(R.string.anime_card_seen_times, anime.vecesVisto)
    } else {
        ""
    }

    val itemDesc = stringResource(R.string.anime_card_item_desc, anime.numero, anime.nombre, chipDesc)
    val positionText = stringResource(R.string.anime_card_position, anime.numero)

    val isSeenMultipleTimes = anime.vecesVisto > 1
    val baseColor = if (isSeenMultipleTimes) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val targetColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        baseColor
    }

    val backgroundColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = if (isHighlighted) 150 else 1000),
        label = "cardHighlightAnimation"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { contentDescription = itemDesc },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = positionText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "  ${anime.nombre}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        if (isSeenMultipleTimes) {
            AssistChip(
                onClick = { onChipClick?.invoke() },
                label = {
                    Text(
                        text = "x${anime.vecesVisto}",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = null
            )
        }

        IconButton(onClick = { onCopy(anime.nombre) }) {
            Icon(
                painter = painterResource(R.drawable.ic_content_copy),
                contentDescription = stringResource(R.string.anime_card_copy),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = { onSearchWeb(anime.nombre) }) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.anime_card_search_google),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.anime_card_delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}