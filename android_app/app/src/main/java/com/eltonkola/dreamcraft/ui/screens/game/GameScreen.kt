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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.composables.ChevronLeft
import com.composables.Container
import com.composables.Play
import com.composables.SquarePen
import com.eltonkola.dreamcraft.data.startGame
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


    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(projectName) },
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp), // Added this line
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(ChevronLeft, contentDescription = "Back")
                    }
                },
                actions = {

                    IconButton(onClick = {
                        navController.navigate("localModels")
                    }) {
                        Icon(Container, contentDescription = "Local Models")
                    }

                    IconButton(onClick = {
                        navController.navigate("editor/${projectName}")
                    }) {
                        Icon(SquarePen, contentDescription = "Edit")
                    }

                    IconButton(onClick = {
                        context.startGame(projectName)
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
                uiState = uiState,
                onDismissError = viewModel::resetState,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            GenerateButton(
                uiState = uiState,
                onGenerateClick = {
                    viewModel.generateGame(it)
                }
            )
        }
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
