// File: LocalModelManagerDialog.kt

package com.eltonkola.dreamcraft.remote.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.PackagePlus
import com.composables.Trash2
import com.eltonkola.dreamcraft.core.data.RemoteFileDto
import com.eltonkola.dreamcraft.remote.data.ActiveAiConfig
import com.eltonkola.dreamcraft.remote.data.CloudServiceType
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun LocalModelManagerDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocalModelManagerViewModel = hiltViewModel()
) {
    if (showDialog) {
        val snackbarHostState = remember { SnackbarHostState() }
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = modifier.padding(16.dp),
            title = { Text("🤖 AI Model Manager", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    containerColor = Color.Transparent,
                ) { paddingValues ->
                    LocalModelManagerContent(
                        modifier = Modifier.padding(paddingValues),
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState,
                    )
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
        )
    }
}

@Composable
private fun LocalModelManagerContent(
    modifier: Modifier = Modifier,
    viewModel: LocalModelManagerViewModel,
    snackbarHostState: SnackbarHostState
) {
    // Collect all new state from the ViewModel
    val activeAiConfig by viewModel.activeAiConfig.collectAsState()
    val savedApiKeys by viewModel.savedApiKeys.collectAsState()
    val localFiles by viewModel.localFiles.collectAsState()
    val remoteFiles by viewModel.remoteFiles.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()

    val scope = rememberCoroutineScope()
    var showApiKeyDialog by remember { mutableStateOf<CloudServiceType?>(null) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }

    // LaunchedEffect for download notifications
    LaunchedEffect(downloadState) {
        when (val state = downloadState) {
            is DownloadState.Finished -> {
                scope.launch { snackbarHostState.showSnackbar("Download complete: ${state.file.name}") }
                viewModel.resetDownloadState()
            }
            is DownloadState.Error -> {
                scope.launch { snackbarHostState.showSnackbar("Error: ${state.message}", withDismissAction = true) }
                viewModel.resetDownloadState()
            }
            else -> Unit
        }
    }

    val isDownloading = downloadState is DownloadState.Downloading

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> if (uri != null && !isDownloading) viewModel.importFileFromUri(uri) }
    )

    // --- Main UI Column ---
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // --- API Key Entry Dialog ---
        if (showApiKeyDialog != null) {
            ApiKeyEntryDialog(
                serviceType = showApiKeyDialog!!,
                onDismiss = { showApiKeyDialog = null },
                onSave = { service, key ->
                    viewModel.saveApiKey(service, key)
                    showApiKeyDialog = null
                }
            )
        }

        // --- Delete Local File Confirmation Dialog ---
        if (fileToDelete != null) {
            AlertDialog(
                onDismissRequest = { fileToDelete = null },
                title = { Text("Delete Model") },
                text = { Text("Are you sure you want to delete \"${fileToDelete?.nameWithoutExtension}\"?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            fileToDelete?.let { viewModel.deleteLocalFile(it) }
                            fileToDelete = null
                        }
                    ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { fileToDelete = null }) { Text("Cancel") } }
            )
        }

        // --- Active Integration Section ---
        Text("Active Integration", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        // --- Cloud Services ---
        Text("Cloud Services", style = MaterialTheme.typography.titleSmall)
        CloudServiceType.values().filter { it != CloudServiceType.FAKE }.forEach { serviceType ->
            val apiKey = savedApiKeys[serviceType]
            val isSelected = activeAiConfig is ActiveAiConfig.Cloud && (activeAiConfig as ActiveAiConfig.Cloud).serviceType == serviceType

            CloudServiceCard(
                serviceType = serviceType,
                isSelected = isSelected,
                hasApiKey = apiKey != null,
                onClick = {
                    if (apiKey != null) {
                        viewModel.selectAi(ActiveAiConfig.Cloud(serviceType, apiKey))
                    } else {
                        showApiKeyDialog = serviceType
                    }
                },
                onEditClick = { showApiKeyDialog = serviceType },
                onDeleteClick = { viewModel.deleteApiKey(serviceType) }
            )
        }

        Spacer(Modifier.height(16.dp))

        // --- Local Models ---
        Text("Local Models", style = MaterialTheme.typography.titleSmall)
        if (localFiles.isEmpty()) {
            Text("No local models found. Use the download or import options below.", style = MaterialTheme.typography.bodySmall)
        }
        localFiles.forEach { file ->
            val isSelected = activeAiConfig is ActiveAiConfig.Local && (activeAiConfig as ActiveAiConfig.Local).llmPath == file.absolutePath
            LocalModelCard(
                file = file,
                isSelected = isSelected,
                onSelect = { viewModel.selectAi(ActiveAiConfig.Local(it.absolutePath, it.nameWithoutExtension)) },
                onDelete = { fileToDelete = it }
            )
        }

        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        // --- Available Downloads Section ---
        Text("Available Downloads", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (isDownloading) {
            val progress = (downloadState as DownloadState.Downloading).progress
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                Text("Downloading... $progress%", style = MaterialTheme.typography.bodySmall)
            }
        }
        remoteFiles.forEach { remote ->
            DownloadableItem(
                remoteFile = remote,
                isDownloading = isDownloading,
                onDownloadClick = { viewModel.downloadRemoteFile(remote) }
            )
        }

        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(8.dp))

        // --- Import Section ---
        OutlinedButton(
            enabled = !isDownloading,
            onClick = { filePickerLauncher.launch("application/octet-stream") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📥 Import .task file")
        }
    }
}

// --- Reusable UI Components for the Dialog ---

@Composable
private fun CloudServiceCard(
    serviceType: CloudServiceType,
    isSelected: Boolean,
    hasApiKey: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isSelected, onClick = onClick, enabled = hasApiKey)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "☁️ ${serviceType.name.replace('_', ' ')}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    text = if (hasApiKey) "API Key is set" else "API Key not set",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasApiKey) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                )
            }
            if (hasApiKey) {
                IconButton(onClick = onDeleteClick) {
                    Icon(Trash2, contentDescription = "Delete API Key", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onEditClick) {
                Icon(PackagePlus, contentDescription = if (hasApiKey) "Edit API Key" else "Add API Key")
            }
        }
    }
}

@Composable
private fun LocalModelCard(
    file: File,
    isSelected: Boolean,
    onSelect: (File) -> Unit,
    onDelete: (File) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelect(file) },
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isSelected, onClick = { onSelect(file) })
            Column(modifier = Modifier.weight(1f)) {
                Text("💾 ${file.nameWithoutExtension}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("${file.length() / (1024 * 1024)} MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { onDelete(file) }, enabled = !isSelected) {
                Icon(Trash2, contentDescription = "Delete Model", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DownloadableItem(remoteFile: RemoteFileDto, isDownloading: Boolean, onDownloadClick: () -> Unit) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(remoteFile.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        OutlinedButton(enabled = !isDownloading, onClick = onDownloadClick) {
            Text("Download", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiKeyEntryDialog(
    serviceType: CloudServiceType,
    onDismiss: () -> Unit,
    onSave: (CloudServiceType, String) -> Unit
) {
    var textState by remember { mutableStateOf(TextFieldValue("")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter ${serviceType.name} API Key") },
        text = {
            TextField(
                value = textState,
                onValueChange = { textState = it },
                label = { Text("API Key") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(serviceType, textState.text) },
                enabled = textState.text.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
