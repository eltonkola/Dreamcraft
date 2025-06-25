package com.eltonkola.dreamcraft.data

import com.eltonkola.dreamcraft.core.ProjectConfig
import com.eltonkola.dreamcraft.remote.data.AiIntegration
import com.eltonkola.dreamcraft.ui.screens.game.editor.FileItem

interface AiRepository {
    suspend fun generateGame(
        integration: AiIntegration,
        prompt: String,
        projectName: String,
        config: ProjectConfig,
        file: FileItem?): Result<String>
}
