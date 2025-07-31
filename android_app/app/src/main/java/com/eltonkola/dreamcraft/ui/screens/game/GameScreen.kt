// File: GameScreen.kt

package com.eltonkola.dreamcraft.ui.screens.game

import android.R.id.message
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.composables.ChevronLeft
import com.composables.Container
import com.composables.Play
import com.composables.SendHorizontal
import com.composables.SquarePen
import com.eltonkola.dreamcraft.core.ProjectConfig
import com.eltonkola.dreamcraft.core.ProjectType
import com.eltonkola.dreamcraft.core.loadProjectMetadata
import com.eltonkola.dreamcraft.core.model.FileItem
import com.eltonkola.dreamcraft.data.openHtmlViewer
import com.eltonkola.dreamcraft.data.startGame
import com.eltonkola.dreamcraft.remote.data.ActiveAiConfig
import com.eltonkola.dreamcraft.remote.data.CloudServiceType
import com.eltonkola.dreamcraft.remote.ui.LocalModelManagerDialog
import com.eltonkola.dreamcraft.ui.screens.game.editor.scanFilesFromPath
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    projectName: String,
    navController: NavHostController,
    viewModel: GameViewModel = hiltViewModel()
) {
    // Collect the state from the fully refactored ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val activeAiConfig by viewModel.activeAiConfig.collectAsState()

    var showAiModelDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // State for the file list and selection
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var selectedFile by remember { mutableStateOf<FileItem?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var config by remember { mutableStateOf<ProjectConfig?>(null) }

    LaunchedEffect(key1 = projectName) {
        files = scanFilesFromPath(context, projectName)
        val projectsDir = File(context.filesDir, "projects")
        val loadedConfig = loadProjectMetadata(File(projectsDir, projectName))
        config = loadedConfig
        // Set the initial selected file based on the project config
        selectedFile = files.firstOrNull { it.name == loadedConfig?.defaultName } ?: files.firstOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(projectName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(ChevronLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(onClick = { showAiModelDialog = true }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Container, contentDescription = "AI Models")
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(activeAiConfig.getDisplayName())
                        }
                    }
                    IconButton(onClick = { navController.navigate("editor/${projectName}") }) {
                        Icon(SquarePen, contentDescription = "Edit")
                    }
                    IconButton(onClick = {
                        config?.let { cfg ->
                            if (cfg.type == ProjectType.LOVE2D) {
                                context.startGame(projectName)
                            } else {
                                val htmlFile = File(context.filesDir, "projects/$projectName/${cfg.defaultName}")
                                context.openHtmlViewer(htmlFile)
                            }
                        }
                    }) {
                        Icon(Play, contentDescription = "Play")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime)
            ) {
                StatusCard(
                    activeAi = activeAiConfig,
                    uiState = uiState,
                    onDismissError = viewModel::resetState,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // File selection dropdown
                if (files.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Target File:",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            TextField(
                                value = selectedFile?.name ?: "Select a file",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                files.forEach { file ->
                                    DropdownMenuItem(
                                        text = { Text(file.name) },
                                        onClick = {
                                            selectedFile = file
                                            expanded = false
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
                    onGenerateClick = { prompt ->
                        config?.let { cfg ->
                            selectedFile?.let { file ->
                                viewModel.generateGame(prompt, cfg, file)
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        // Main content area
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { chatMessage ->
                MessageBubble(chatMessage)
            }
        }

        // The dialog call is now simplified and self-contained
        LocalModelManagerDialog(
            showDialog = showAiModelDialog,
            onDismiss = { showAiModelDialog = false }
        )
    }
}

/**
 * A helper extension function to get a user-friendly display name from the ActiveAiConfig.
 */
private fun ActiveAiConfig.getDisplayName(): String {
    return when (this) {
        is ActiveAiConfig.Cloud -> when (this.serviceType) {
            CloudServiceType.GROQ -> "Groq"
            CloudServiceType.OPENAI -> "OpenAI"
            CloudServiceType.GEMINI -> "Gemini"
            CloudServiceType.CLAUDE -> "Claude"
            CloudServiceType.FAKE -> "Fake"
        }

        is ActiveAiConfig.Local -> this.llmName
        is ActiveAiConfig.None -> "None"
    }
}


/**
 * A composable for displaying a single chat message bubble.
 */
@Composable
fun MessageBubble(message: ChatMessage){
    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
        ) {
            Card(
                modifier = Modifier.widthIn(max = 300.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (message.isFromUser) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(12.dp),
                    color = if (message.isFromUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }

