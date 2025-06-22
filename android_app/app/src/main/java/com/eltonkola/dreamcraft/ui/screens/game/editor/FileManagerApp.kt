package com.eltonkola.dreamcraft.ui.screens.game.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.navigation.NavHostController
import com.composables.FileAudio2
import com.composables.FileCode
import com.composables.FileImage
import com.composables.FilePen
import com.composables.Import
import com.composables.Menu
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
        val file = File(uri!!.path)
        file.writeText(content)
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

    LaunchedEffect(projectName) {
        files = scanFilesFromPath(context, projectName)
    }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // In a real app, you'd read the file content here
            val fileName = "imported_file_${System.currentTimeMillis()}"
            val fileType = getFileType(fileName)
            val newFile = FileItem(
                id = fileName,
                name = fileName,
                type = fileType,
                uri = uri
            )
            files = files + newFile
        }
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
                    files = files.filter { it.id != file.id }
                    if (selectedFile?.id == file.id) {
                        selectedFile = null
                    }
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
                            enabled = selectedFile !=null && selectedFile?.isSaved == false,
                            onClick = {
                                scope.launch {
                                    selectedFile?.saveFile()
                                    selectedFile = selectedFile?.copy(isSaved = true)
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
                    FileViewer(
                        file = file,
                        onFileContentChanged = { content ->
                            files = files.map {
                                if (it.id == file.id) it.copy(content = content, isSaved = false) else it
                            }
                            selectedFile = selectedFile?.copy(content = content, isSaved = false)
                        }
                    )
                } ?: WelcomeScreen()
            }
        }
    }
}

@Composable
fun FileDrawerContent(
    files: List<FileItem>,
    selectedFile: FileItem?,
    onFileSelected: (FileItem) -> Unit,
    onImportFile: () -> Unit,
    onDeleteFile: (FileItem) -> Unit
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

            // Import button
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

            // File list
            LazyColumn {
                items(files) { file ->
                    FileListItem(
                        file = file,
                        isSelected = selectedFile?.id == file.id,
                        onClick = { onFileSelected(file) },
                        onDelete = { onDeleteFile(file) }
                    )
                }
            }
        }
    }
}

@Composable
fun FileListItem(
    file: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
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
                .padding(12.dp),
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
                    text = file.type.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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



