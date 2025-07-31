// File: GameViewModel.kt

package com.eltonkola.dreamcraft.ui.screens.game // Or your relevant UI package

import android.R.attr.name
import android.util.Log.e
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eltonkola.dreamcraft.core.ProjectConfig
import com.eltonkola.dreamcraft.core.model.FileItem
import com.eltonkola.dreamcraft.data.AiRepository
import com.eltonkola.dreamcraft.remote.data.ActiveAiConfig
import com.eltonkola.dreamcraft.remote.data.AiApiServiceFactory
import com.eltonkola.dreamcraft.remote.data.AiResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Define the UI states for this screen
sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val response: AiResponse, val selectedFile: FileItem) : UiState
    data class Error(val message: String) : UiState
}

// Define a simple data class for chat messages
data class ChatMessage(
    val text: String,
    val isFromUser: Boolean
)

@HiltViewModel
class GameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val aiPreferencesRepository: com.eltonkola.dreamcraft.remote.data.AiPreferencesRepository, // For READING the active config
    private val aiApiServiceFactory: AiApiServiceFactory, // For CREATING the correct AI engine
    private val aiRepository: AiRepository
) : ViewModel() {

    val projectName: String = savedStateHandle["projectName"] ?: "Untitled Project"

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // NEW: The source of truth for the active AI is now the repository.
    // We observe it as a StateFlow.
    val activeAiConfig: StateFlow<ActiveAiConfig> = aiPreferencesRepository.activeAiConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ActiveAiConfig.None
        )

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    /**
     * This is the core, refactored function. It uses the factory to get the
     * correct service and then calls it.
     */
    fun generateGame(prompt: String, config: ProjectConfig, selectedFile: FileItem) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _messages.update { it + ChatMessage(prompt, true) }

            // 1. Get the current, user-selected AI configuration from our state flow.
            val currentConfig = activeAiConfig.value

            // 2. Use the factory to create the correct service (Groq, Local, OpenAI, etc.).
            val service = aiApiServiceFactory.create(currentConfig)

            // 3. Check if a service could be created. If not, no AI is configured.
            if (service == null) {
                return@launch
            }

            // 4. Use a standard try-catch block to handle network/inference errors.
                val response = aiRepository.generateGame( activeAiConfig.value, prompt, projectName, config, selectedFile )
                val result = response.getOrNull()
                if(response.isSuccess && result !=null){
                    _uiState.value = UiState.Success(result, selectedFile)
                    _messages.update { it + ChatMessage("✅ Success! Code has been generated.", false) }

                }else{
                    val errorMessage = response.exceptionOrNull()?.message ?: "An unknown error occurred"
                    _uiState.value = UiState.Error(errorMessage)
                    _messages.update { it + ChatMessage(errorMessage, false) }

                }


        }
    }

    /**
     * Resets the UI state, for example, after a success or error has been handled.
     */
    fun resetState() {
        _uiState.value = UiState.Idle
    }

    // The old 'setActiveAi' method is now DELETED. That logic belongs in the
    // LocalModelManagerViewModel, which saves the choice to the repository.
}