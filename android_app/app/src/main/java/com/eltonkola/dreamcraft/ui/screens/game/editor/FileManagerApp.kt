package com.eltonkola.dreamcraft.ui.screens.game.editor

import android.R.id.message
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.composables.FileAudio2
import com.composables.FileCode
import com.composables.FileImage
import com.composables.FilePen // Used for rename icon
import com.composables.Import
import com.composables.Menu
import com.composables.PenLine
import com.composables.Save
import com.composables.Trash2
import kotlinx.coroutines.launch
import java.io.File

// Data classes
data class FileItem(
    val id: String,
    val name: String,
    val type: FileType,
    val content: String = "",
    val uri: Uri? = null,
    val isSaved: Boolean = true
){
    fun saveFile(){
        uri?.path?.let {
            val file = File(it)
            file.writeText(content)
        }
    }
}

enum class FileType(val icon: ImageVector, val extensions: List<String>) {
    LUA(FileCode, listOf("lua")),
    TEXT(FilePen, listOf("txt", "md", "json", "xml", "html", "css", "js", "py", "java", "kt")),
    IMAGE(FileImage, listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")),
    AUDIO(FileAudio2, listOf("mp3", "wav", "ogg", "m4a", "flac"))
}

fun getFileType(fileName: String): FileType {
    val extension = fileName.substringAfterLast(".").lowercase()
    return FileType.values().find { it.extensions.contains(extension) } ?: FileType.TEXT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerApp(
    projectName: String,
    navController: NavHostController,
) {
    var files by remember { mutableStateOf(listOf<FileItem>()) }
    var selectedFile by remember { mutableStateOf<FileItem?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var renamingFile by remember { mutableStateOf<FileItem?>(null) }

    fun refreshFiles() {
        files = scanFilesFromPath(context, projectName)
    }

    LaunchedEffect(projectName) {
        refreshFiles()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val originalFileName = it.lastPathSegment ?: "imported_file_${System.currentTimeMillis()}"
            val projectDir = File(context.filesDir, "projects/$projectName")
            if (!projectDir.exists()) projectDir.mkdirs()

            var destinationFile = File(projectDir, originalFileName)
            var counter = 1
            var uniqueFileName = originalFileName
            while (destinationFile.exists()) {
                val nameWithoutExtension = originalFileName.substringBeforeLast(".")
                val extension = originalFileName.substringAfterLast(".", "")
                uniqueFileName = if (extension.isNotEmpty()) {
                    "${nameWithoutExtension}_${counter}.$extension"
                } else {
                    "${nameWithoutExtension}_${counter}"
                }
                destinationFile = File(projectDir, uniqueFileName) // Update destinationFile for next check
                counter++
            }


            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    File(projectDir, uniqueFileName).outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                refreshFiles()
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle import error (e.g., show a toast)
            }
        }
    }

    fun handleRenameFile(fileToRename: FileItem, newName: String) {
        if (newName.isBlank() || newName == fileToRename.name) {
            renamingFile = null
            return
        }

        fileToRename.uri?.path?.let { currentPath ->
            val currentFile = File(currentPath)
            val parentDir = currentFile.parentFile
            if (parentDir != null) {
                val newFile = File(parentDir, newName)
                if (newFile.exists()) {
                    // Handle file already exists error (e.g., show a toast)
                    println("Error: File with new name already exists.")
                    renamingFile = null // close dialog
                    return
                }
                if (currentFile.renameTo(newFile)) {
                    refreshFiles()
                    // Update selectedFile if it was the one renamed
                    if (selectedFile?.id == fileToRename.id) {
                        selectedFile = files.find { it.uri?.path == newFile.path }
                    }
                } else {
                    // Handle rename failure (e.g., show a toast)
                     println("Error: Could not rename file.")
                }
            }
        }
        renamingFile = null
    }


    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            FileDrawerContent(
                files = files,
                selectedFile = selectedFile,
                onFileSelected = { file ->
                    selectedFile = file
                    scope.launch { drawerState.close() }
                },
                onImportFile = {
                    filePickerLauncher.launch("*/*")
                },
                onDeleteFile = { file ->
                    file.uri?.path?.let {
                        val actualFile = File(it)
                        if (actualFile.delete()) {
                            refreshFiles()
                            if (selectedFile?.id == file.id) {
                                selectedFile = null
                            }
                        } else {
                            // Handle deletion failure
                             println("Error: Could not delete file.")
                        }
                    }
                },
                onCreateFile = { fileName ->
                    try {
                        val file = File(context.filesDir, "projects/$projectName/$fileName.lua" )
                        // Ensure parent directory exists
                        file.parentFile?.mkdirs()
                        if (file.createNewFile()) {
                           refreshFiles()
                        } else {
                            // Handle file creation error (e.g. file already exists)
                             println("Error: Could not create file or file already exists.")
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onRenameFileRequest = { file ->
                    renamingFile = file
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(selectedFile?.name ?: "File Manager")
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(
                            enabled = selectedFile != null && selectedFile?.isSaved == false,
                            onClick = {
                                scope.launch {
                                    selectedFile?.saveFile()
                                    // Optimistically update UI, or re-fetch/verify
                                    selectedFile = selectedFile?.copy(isSaved = true)
                                     // refreshFiles() // if saveFile might change other aspects or to be very safe
                                }
                            }
                        ) {
                            Icon(Save, contentDescription = "Save")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                selectedFile?.let { file ->
                    // Attempt to read content only if URI is valid and file exists
                    val contentToShow = remember(file.uri, file.isSaved) { // Re-read if uri or save state changes externally
                        file.uri?.path?.let { filePath ->
                            try {
                                File(filePath).readText()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                "Error reading file: ${e.localizedMessage}"
                            }
                        } ?: file.content // Fallback to existing content if no URI
                    }

                    FileViewer(
                        file = file.copy(content = contentToShow), // Pass the read content
                        onFileContentChanged = { newContent ->
                            files = files.map {
                                if (it.id == file.id) it.copy(content = newContent, isSaved = false) else it
                            }
                            selectedFile = selectedFile?.copy(content = newContent, isSaved = false)
                        }
                    )
                } ?: WelcomeScreen()
            }
        }
    }

    renamingFile?.let { fileToRename ->
        RenameFileDialog(
            file = fileToRename,
            onConfirm = { newName ->
                handleRenameFile(fileToRename, newName)
            },
            onDismiss = {
                renamingFile = null
            }
        )
    }
}

@Composable
fun FileDrawerContent(
    files: List<FileItem>,
    selectedFile: FileItem?,
    onFileSelected: (FileItem) -> Unit,
    onImportFile: () -> Unit,
    onDeleteFile: (FileItem) -> Unit,
    onCreateFile: (String) -> Unit,
    onRenameFileRequest: (FileItem) -> Unit // New callback
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Files",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = onImportFile,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Icon(Import, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import File")
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(files) { file ->
                    FileListItem(
                        file = file,
                        isSelected = selectedFile?.id == file.id,
                        onClick = { onFileSelected(file) },
                        onDelete = { onDeleteFile(file) },
                        onRename = { onRenameFileRequest(file) } // Pass the callback
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            var newFileName by remember { mutableStateOf("") }
            TextField(
                value = newFileName,
                onValueChange = { newFileName = it },
                label = { Text("Enter new file name (.lua)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("example.lua")}
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                enabled = newFileName.isNotBlank() && newFileName.endsWith(".lua"),
                onClick = {
                    if (newFileName.isNotBlank()) {
                        onCreateFile(newFileName.removeSuffix(".lua"))
                        newFileName = ""
                    }
                }) {
                Text("Create Lua File")
            }
        }
    }
}

@Composable
fun FileListItem(
    file: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = file.type.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = file.type.name.lowercase().replaceFirstChar { it.uppercase() }, // e.g. Lua, Text
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            IconButton(onClick = onRename) {
                Icon(
                    PenLine,
                    contentDescription = "Rename",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Trash2,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun RenameFileDialog(
    file: FileItem,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember(file.name) { mutableStateOf(file.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename File") },
        text = {
            TextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New file name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(newName) }) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
