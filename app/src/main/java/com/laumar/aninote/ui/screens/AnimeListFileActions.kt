package com.laumar.aninote.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.laumar.aninote.viewmodel.AnimeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AnimeListFileActions(
    val launchImport: () -> Unit,
    val launchExportTxt: () -> Unit,
    val launchExportJson: () -> Unit
)

@Composable
fun rememberAnimeListFileActions(
    viewModel: AnimeViewModel,
    onImportLoaded: (content: String, isJson: Boolean) -> Unit
): AnimeListFileActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                        reader.readText()
                    }
                }
                if (content != null) {
                    val trimmed = content.trim()
                    val mimeType = context.contentResolver.getType(it)
                    val isJson = (mimeType?.contains("json", ignoreCase = true) == true) ||
                            (trimmed.startsWith("{") && trimmed.endsWith("}"))

                    onImportLoaded(content, isJson)
                }
            }
        }
    }

    val exportTxtLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val exportContent = withContext(Dispatchers.Default) {
                    viewModel.getExportTxt()
                }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer ->
                        writer.write(exportContent)
                    }
                }
            }
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val exportContent = withContext(Dispatchers.Default) {
                    viewModel.getExportJson()
                }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer ->
                        writer.write(exportContent)
                    }
                }
            }
        }
    }

    return AnimeListFileActions(
        launchImport = { importLauncher.launch(arrayOf("text/plain", "application/json")) },
        launchExportTxt = { exportTxtLauncher.launch("anime_list.txt") },
        launchExportJson = { exportJsonLauncher.launch("anime_list.json") }
    )
}
