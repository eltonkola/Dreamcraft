package com.eltonkola.dreamcraft.data

import com.eltonkola.dreamcraft.ui.screens.game.editor.FileItem

interface GroqRepository {
    suspend fun generateGame(prompt: String, projectName: String, file: FileItem?): Result<String>
}

data class AiResponse(
    val thought: String?,
    val code: String
)
