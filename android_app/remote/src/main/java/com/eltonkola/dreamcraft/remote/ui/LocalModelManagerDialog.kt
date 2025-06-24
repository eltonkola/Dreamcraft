package com.eltonkola.dreamcraft.remote.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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

data class RemoteFileDto(
    val name: String,
    val downloadUrl: String
)

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
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            ),
            modifier = modifier.padding(16.dp),
            title = {
                Text(
                    text = "🤖 AI Model Manager",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                LocalModelManagerContent(
                    viewModel = viewModel,
                    currentActiveAi = currentActiveAi,
                    onAiSelected = onAiSelected
                )
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
    viewModel: LocalModelManagerViewModel,
    currentActiveAi: AiIntegration?,
    onAiSelected: (AiIntegration) -> Unit
) {
    val localFiles by viewModel.localFiles.collectAsState()
    val remoteFiles by viewModel.remoteFiles.collectAsState()

    val context = LocalContext.current
    val internalFilesDir = context.filesDir
    val scope = rememberCoroutineScope()

    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }


    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    val name = getFileName(context, uri)
                    val destFile = File(internalFilesDir, name ?: "imported_${System.currentTimeMillis()}.task")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    viewModel.refreshLocalFiles()
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // AI Integration Selection Section
        Text("Active Integration", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        // GROQ Option (always available)
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
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
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


            // Delete confirmation dialog
            if (showDeleteDialog && fileToDelete != null) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteDialog = false
                        fileToDelete = null
                    },
                    title = {
                        Text("Delete Model")
                    },
                    text = {
                        Text("Are you sure you want to delete \"${fileToDelete?.nameWithoutExtension}\"? This action cannot be undone.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                fileToDelete?.let { file ->
                                    viewModel.deleteLocalFile(file)
                                    // If the deleted file was the active AI, switch back to GROQ
                                    if (currentActiveAi is AiIntegration.LOCAL &&
                                        currentActiveAi.llmPath == file.absolutePath) {
                                        onAiSelected(AiIntegration.GROQ())
                                    }
                                }
                                showDeleteDialog = false
                                fileToDelete = null
                            }
                        ) {
                            Text(
                                "Delete",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                fileToDelete = null
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

        }

        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        // Available Downloads Section
        Text("Available Downloads", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

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
            onClick = {
                filePickerLauncher.launch("application/octet-stream")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📥 Import .task file")
        }
    }
}

fun getFileName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        cursor.getString(nameIndex)
    }
}