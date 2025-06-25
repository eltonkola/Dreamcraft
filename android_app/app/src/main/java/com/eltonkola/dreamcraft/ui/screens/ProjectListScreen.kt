package com.eltonkola.dreamcraft.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.composables.ChevronRight
import com.composables.FolderPlus
import com.composables.Gamepad2
import com.composables.Trash2
import com.eltonkola.dreamcraft.core.ProjectConfig
import com.eltonkola.dreamcraft.core.projectTypes
import com.eltonkola.dreamcraft.data.local.DreamProject
import com.eltonkola.dreamcraft.data.local.createProject
import com.eltonkola.dreamcraft.data.local.deleteProject
import com.eltonkola.dreamcraft.data.local.loadProjects
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(navController: NavHostController) {
    val context = LocalContext.current
    var projects by remember { mutableStateOf(listOf<DreamProject>()) }
    var showDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Load projects on first composition
    LaunchedEffect(Unit) {
        projects = loadProjects(context)
    }

    if (showDialog) {


        NewProjectDialog(
            showDialog = showDialog,
            onDismiss = { showDialog = false },
            onCreate = { name, config ->
                scope.launch {
                    createProject(context = context, projectName = name, type = config, file = null)
                    projects = loadProjects(context)
                }
            }
        )

    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row (
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Projects",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            FloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(FolderPlus, contentDescription = "Add Project")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (projects.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No projects yet. Create your first project!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items (projects) { project ->
                    ProjectCard(
                        project = project,
                        onClick = {
                            navController.navigate("game/${project.name}")
                        },
                        onDelete = {
                            scope.launch {
                                deleteProject(context, project.name)

                                projects = loadProjects(context)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    projectName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Project") },
        text = { Text("Are you sure you want to delete the project \"$projectName\"? This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
fun NewProjectDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, ProjectConfig) -> Unit
) {
    if (!showDialog) return
    var newProjectName by remember { mutableStateOf("") }
    var selectedConfig by remember { mutableStateOf(projectTypes.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Project") },
        text = {
            Column {
                OutlinedTextField(
                    value = newProjectName,
                    onValueChange = { newProjectName = it },
                    label = { Text("Project Name") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Project Type", style = MaterialTheme.typography.labelMedium)

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    projectTypes.forEach { config ->
                        FilterChip(
                            selected = config == selectedConfig,
                            onClick = { selectedConfig = config },
                            label = { Text(config.name) },
                            leadingIcon = {
                                Icon(config.icon, contentDescription = config.name)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newProjectName.isNotBlank()) {
                        onCreate(newProjectName, selectedConfig)
                        newProjectName = ""
                        onDismiss()
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
fun ProjectCard(project: DreamProject, onClick: () -> Unit, onDelete : () -> Unit ) {
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val state = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
        positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
    )


    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            projectName = project.name,
            onDismiss = {
                showDeleteDialog = false
                scope.launch {
                    state.reset()
                }

            },
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            }
        )
    }



    SwipeToDismissBox(
        state = state,
        onDismiss = {
            showDeleteDialog = true
        },
        backgroundContent = {
            val color = when (state.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> Color.Red
                SwipeToDismissBoxValue.StartToEnd -> Color.Red
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }

            Box(
                modifier = Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp),
                    contentAlignment = when (state.dismissDirection) {
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    else -> Alignment.Center
                }
            ) {
                Icon(
                    imageVector = Trash2,
                    contentDescription = "Delete",
                    tint = Color.White
                )
            }
        }
    ) {

            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                onClick = onClick
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        project.config.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Created ${project.timeAgo()} • ${project.config.name}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }


                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        ChevronRight,
                        contentDescription = null
                    )
                }
            }
        }
}
