package com.laumar.aninote.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.laumar.aninote.R
import com.laumar.aninote.viewmodel.AnimeUi
import java.net.URLEncoder

@Composable
fun AnimeCard(
    anime: AnimeUi,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val chipDesc = if (anime.vecesVisto > 1) {
        stringResource(R.string.anime_card_seen_times, anime.vecesVisto)
    } else {
        ""
    }

    val itemDesc = stringResource(R.string.anime_card_item_desc, anime.numero, anime.nombre, chipDesc)
    val positionText = stringResource(R.string.anime_card_position, anime.numero)

    val isSeenMultipleTimes = anime.vecesVisto > 1
    val backgroundColor = if (isSeenMultipleTimes) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surface
    }

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
                onClick = {},
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

        IconButton(onClick = { copyToClipboard(context, anime.nombre) }) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = stringResource(R.string.anime_card_copy),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = { searchInGoogle(context, anime.nombre) }) {
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

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("anime_name", text)
    clipboard.setPrimaryClip(clip)
}

private fun searchInGoogle(context: Context, query: String) {
    val encoded = URLEncoder.encode(query, "UTF-8")
    val intent = Intent(Intent.ACTION_VIEW, "https://www.google.com/search?q=$encoded".toUri())
    context.startActivity(intent)
}