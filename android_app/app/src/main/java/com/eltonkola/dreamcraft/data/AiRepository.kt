package com.eltonkola.dreamcraft.data

import com.eltonkola.dreamcraft.core.ProjectConfig
import com.eltonkola.dreamcraft.core.model.FileItem
import com.eltonkola.dreamcraft.remote.data.ActiveAiConfig
import com.eltonkola.dreamcraft.remote.data.AiResponse

interface AiRepository {
    suspend fun generateGame(
        aiConfig: ActiveAiConfig,
        prompt: String,
        projectName: String,
        config: ProjectConfig,
        file: FileItem?): Result<AiResponse>
}
