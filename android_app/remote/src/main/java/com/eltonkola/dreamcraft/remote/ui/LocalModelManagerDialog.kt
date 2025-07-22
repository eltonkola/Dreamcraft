package com.eltonkola.dreamcraft.remote.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.Trash2
import com.eltonkola.dreamcraft.remote.data.AiIntegration
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

// The data class can be removed from this file if it's defined elsewhere
// data class RemoteFileDto(
//     val name: String,
//     val downloadUrl: String
// )

@Composable
fun LocalModelManagerDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocalModelManagerViewModel = hiltViewModel(),
    currentActiveAi: AiIntegration? = null,
    onAiSelected: (AiIntegration) -> Unit = {}
) {
    if (showDialog) {
        // Use a SnackbarHostState to show download results
        val snackbarHostState = remember { SnackbarHostState() }

        // We use a Scaffold inside the AlertDialog to host the Snackbar
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = modifier.padding(16.dp),
            title = {
                Text(
                    text = "🤖 AI Model Manager",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    containerColor = MaterialTheme.colorScheme.background
                ) { paddingValues ->
                    LocalModelManagerContent(
                        modifier = Modifier.padding(paddingValues),
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState, // Pass the host state down
                        currentActiveAi = currentActiveAi,
                        onAiSelected = onAiSelected
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LocalModelManagerContent(
    modifier: Modifier = Modifier,
    viewModel: LocalModelManagerViewModel,
    snackbarHostState: SnackbarHostState, // Receive the host state
    currentActiveAi: AiIntegration?,
    onAiSelected: (AiIntegration) -> Unit
) {
    // 1. COLLECT ALL NECESSARY STATES
    val localFiles by viewModel.localFiles.collectAsState()
    val remoteFiles by viewModel.remoteFiles.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState() // The new state for downloads

    val context = LocalContext.current
    val internalFilesDir = context.filesDir
    val scope = rememberCoroutineScope()

    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 2. LAUNCHED EFFECT TO HANDLE SNACKBARS FOR DOWNLOAD RESULTS
    LaunchedEffect(downloadState) {
        when (val state = downloadState) {
            is DownloadState.Finished -> {
                scope.launch {
                    snackbarHostState.showSnackbar("Download complete: ${state.file.name}")
                }
                viewModel.resetDownloadState() // Reset state to hide snackbar
            }
            is DownloadState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar("Error: ${state.message}", withDismissAction = true)
                }
                viewModel.resetDownloadState()
            }
            else -> { /* Do nothing for Idle or Downloading */ }
        }
    }

    val isDownloading = downloadState is DownloadState.Downloading

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                // Don't allow import while downloading
                if (!isDownloading) {
                    viewModel.importFileFromUri(it)
                }
            }
        }
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // AI Integration Selection Section (No changes needed here)
        Text("Active Integration", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        // GROQ Option
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAiSelected(AiIntegration.GROQ()) },
            colors = CardDefaults.cardColors(
                containerColor = if (currentActiveAi is AiIntegration.GROQ)
                    MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "☁️ Groq",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Cloud-based AI",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RadioButton(
                    selected = currentActiveAi is AiIntegration.GROQ,
                    onClick = { onAiSelected(AiIntegration.GROQ()) }
                )
            }
        }

        // Local Files Section (No changes needed here)
        if (localFiles.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))

            localFiles.forEach { file ->
                val isSelected = currentActiveAi is AiIntegration.LOCAL &&
                        currentActiveAi.llmPath == file.absolutePath

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onAiSelected(
                                AiIntegration.LOCAL(
                                    llmPath = file.absolutePath,
                                    llmName = file.nameWithoutExtension
                                )
                            )
                        }
                        .padding(vertical = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "💾 ${file.nameWithoutExtension}",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(file.length() / (1024 * 1024))} MB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onAiSelected(
                                    AiIntegration.LOCAL(
                                        llmPath = file.absolutePath,
                                        llmName = file.nameWithoutExtension
                                    )
                                )
                            }
                        )
                        IconButton(
                            enabled = !isSelected,
                            onClick = {
                                fileToDelete = file
                                showDeleteDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Trash2,
                                contentDescription = "Delete model",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Delete confirmation dialog (no changes needed here)
            if (showDeleteDialog && fileToDelete != null) {
                // ... (your existing delete dialog code is fine)
            }
        }

        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        // Available Downloads Section
        Text("Available Downloads", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        // 3. ADD THE PROGRESS BAR (Only visible when downloading)
        if (isDownloading) {
            val progress = (downloadState as DownloadState.Downloading).progress
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Downloading... $progress%", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (remoteFiles.isEmpty()) {
            Text(
                "No remote models available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            remoteFiles.forEach { remote ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = remote.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Remote model",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            // 4. DISABLE THE BUTTON WHILE DOWNLOADING
                            enabled = !isDownloading,
                            onClick = {
                                viewModel.downloadRemoteFile(remote)
                            }
                        ) {
                            Text("Download", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(8.dp))

        // Import Section
        OutlinedButton(
            // 5. DISABLE THE IMPORT BUTTON WHILE DOWNLOADING
            enabled = !isDownloading,
            onClick = {
                filePickerLauncher.launch("application/octet-stream")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📥 Import .task file")
        }
    }
}


// No changes needed for this utility function
fun getFileName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        cursor.getString(nameIndex)
    }
}