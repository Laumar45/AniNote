package com.laumar.anilista.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.laumar.anilista.viewmodel.AnimeViewModel

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
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() }
            if (content != null) onImportLoaded(content, it.toString().endsWith(".json", ignoreCase = true))
        }
    }
    val exportTxtLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
        uri?.let { context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer -> writer.write(viewModel.getExportTxt()) } }
    }
    val exportJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri?.let { context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer -> writer.write(viewModel.getExportJson()) } }
    }
    return AnimeListFileActions(
        launchImport = { importLauncher.launch(arrayOf("text/plain", "application/json")) },
        launchExportTxt = { exportTxtLauncher.launch("anime_list.txt") },
        launchExportJson = { exportJsonLauncher.launch("anime_list.json") }
    )
}
