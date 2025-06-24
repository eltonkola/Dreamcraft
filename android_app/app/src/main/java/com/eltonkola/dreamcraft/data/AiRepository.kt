package com.eltonkola.dreamcraft.data

import com.eltonkola.dreamcraft.remote.AiIntegration
import com.eltonkola.dreamcraft.ui.screens.game.editor.FileItem

interface AiRepository {
    suspend fun generateGame(
        integration: AiIntegration,
        prompt: String,
        projectName: String,
        file: FileItem?): Result<String>
}
