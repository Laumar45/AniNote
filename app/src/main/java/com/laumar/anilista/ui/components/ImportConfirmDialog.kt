package com.laumar.anilista.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ImportConfirmDialog(
    isJson: Boolean,
    onReplace: () -> Unit,
    onCombine: () -> Unit,
    onDismiss: () -> Unit
) {
    val formatLabel = if (isJson) "JSON" else "TXT"
    val title = "Importar archivo $formatLabel"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Text(
                "¿Cómo querés importar los animes del archivo $formatLabel?\n\n" +
                "• Reemplazar: borra tu lista actual y carga desde el archivo\n" +
                "• Combinar: agrega los animes del archivo a tu lista actual (se omiten duplicados)"
            )
        },
        confirmButton = {
            TextButton(onClick = onCombine) {
                Text("Combinar")
            }
        },
        dismissButton = {
            TextButton(onClick = onReplace) {
                Text("Reemplazar")
            }
        }
    )
}
