package com.eltonkola.dreamcraft.remote.data

interface AiApiService {
    suspend fun generateGame(prompt: String): AiResponse
}


data class AiResponse(
    val thought: String?,
    val code: String
)

