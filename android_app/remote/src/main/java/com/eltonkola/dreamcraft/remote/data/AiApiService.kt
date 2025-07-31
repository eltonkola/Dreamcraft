package com.eltonkola.dreamcraft.remote.data

import com.eltonkola.dreamcraft.core.ProjectConfig

interface AiApiService {
    suspend fun generate(prompt: String, config: ProjectConfig): AiResponse
}


data class AiResponse(
    val thought: String?,
    val code: String
)

