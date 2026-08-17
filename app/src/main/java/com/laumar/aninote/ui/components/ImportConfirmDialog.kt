package com.laumar.aninote.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.laumar.aninote.R

@Composable
fun ImportConfirmDialog(
    isJson: Boolean,
    onReplace: () -> Unit,
    onCombine: () -> Unit,
    onDismiss: () -> Unit
) {
    val formatLabel = if (isJson) {
        stringResource(R.string.import_format_json)
    } else {
        stringResource(R.string.import_format_txt)
    }
    val title = stringResource(R.string.import_title, formatLabel)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Text(
                stringResource(R.string.import_body, formatLabel)
            )
        },
        confirmButton = {
            TextButton(onClick = onCombine) {
                Text(stringResource(R.string.import_combine))
            }
        },
        dismissButton = {
            TextButton(onClick = onReplace) {
                Text(stringResource(R.string.import_replace))
            }
        }
    )
}
