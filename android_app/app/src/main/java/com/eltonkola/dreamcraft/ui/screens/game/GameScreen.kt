package com.eltonkola.dreamcraft.ui.screens.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.composables.ChevronLeft
import com.composables.Container
import com.composables.Play
import com.composables.SquarePen
import com.eltonkola.dreamcraft.core.ProjectConfig
import com.eltonkola.dreamcraft.core.ProjectType
import com.eltonkola.dreamcraft.core.loadProjectMetadata
import com.eltonkola.dreamcraft.core.projectTypes
import com.eltonkola.dreamcraft.data.openHtmlViewer
import com.eltonkola.dreamcraft.data.startGame
import com.eltonkola.dreamcraft.remote.ui.LocalModelManagerDialog
import com.eltonkola.dreamcraft.ui.screens.game.editor.FileItem
import com.eltonkola.dreamcraft.ui.screens.game.editor.scanFilesFromPath
import java.io.File
import kotlin.collections.reversed

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val filePath: String) : UiState()
    data class Error(val message: String) : UiState()
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(projectName: String,
               navController: NavHostController,
               viewModel: GameViewModel = hiltViewModel()
               ) {


    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()

    val activeAi by viewModel.activeAi.collectAsState()

    var showAiModelDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var selectedFile by remember { mutableStateOf<FileItem?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var config by remember { mutableStateOf<ProjectConfig>(projectTypes.first()) }


    LaunchedEffect(key1 = projectName) {
        files = scanFilesFromPath(context, projectName)
        val projectsDir = File(context.filesDir, "projects")

        config = loadProjectMetadata(File(projectsDir , projectName)) ?: projectTypes.first()

        selectedFile = files.firstOrNull { it.name == config.defaultName } ?: files.firstOrNull()

    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(projectName) },
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(ChevronLeft, contentDescription = "Back")
                    }
                },
                actions = {

                    Button(onClick = {
                        showAiModelDialog = true
                    }) {
                        Row {
                            Icon(Container, contentDescription = "Local Models")
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(activeAi.shortName())
                        }
                    }

                    IconButton(onClick = {
                        navController.navigate("editor/${projectName}")
                    }) {
                        Icon(SquarePen, contentDescription = "Edit")
                    }

                    IconButton(onClick = {
                        if(config.type == ProjectType.LOVE2D){
                            context.startGame(projectName)
                        }else{
                            val htmlFile = File(context.filesDir,  "projects/$projectName/${config.defaultName}")
                            context.openHtmlViewer(htmlFile)
                        }

                    }) {
                        Icon(Play, contentDescription = "Play")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { chatMessage ->
                    MessageBubble(chatMessage)
                }
            }

            StatusCard(
                activeAi = activeAi,
                uiState = uiState,
                onDismissError = viewModel::resetState,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (files.isNotEmpty() && selectedFile != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = selectedFile?.name ?: "Select File",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu (
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        files.forEach { file ->
                            DropdownMenuItem(
                                text = { Text(file.name) },
                                onClick = {
                                    selectedFile = file
                                    expanded = false
                                    // Optional: if you want to clear messages or update ViewModel state when file changes
                                    // viewModel.setCurrentFile(file)
                                }
                            )
                        }
                    }
                }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            GenerateButton(
                uiState = uiState,
                onGenerateClick = {
                    viewModel.generateGame(it, selectedFile, config)
                }
            )
        }


        LocalModelManagerDialog(
            showDialog = showAiModelDialog,
            onDismiss = { showAiModelDialog = false },
            currentActiveAi = activeAi,
            onAiSelected = { selectedAi ->
                viewModel.setActiveAi(selectedAi)
                showAiModelDialog = false
            }
        )

    }
}




@Composable
fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (message.isUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)
