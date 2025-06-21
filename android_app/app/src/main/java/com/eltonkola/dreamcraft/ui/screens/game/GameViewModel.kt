package com.eltonkola.dreamcraft.ui.screens.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eltonkola.dreamcraft.data.GroqRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: GroqRepository
) : ViewModel() {

    val projectName = savedStateHandle["projectName"] ?: ""

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()


    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()



    fun generateGame(prompt: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            _messages.update { it + ChatMessage(prompt, true) }

            repository.generateGame(prompt, projectName)
                .onSuccess { filePath ->
                    _uiState.value = UiState.Success(filePath)

                    _messages.update { it + ChatMessage("Updated main file", false) }

                }
                .onFailure { exception ->
                    _uiState.value = UiState.Error(
                        exception.message ?: "Unknown error occurred"

                    )
                    _messages.update { it + ChatMessage("Error occurred", false) }
                }
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }
}
