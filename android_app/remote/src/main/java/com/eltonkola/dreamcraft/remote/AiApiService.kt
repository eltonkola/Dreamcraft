package com.eltonkola.dreamcraft.remote

interface AiApiService {
    suspend fun generateGame(prompt: String): AiResponse
}


data class AiResponse(
    val thought: String?,
    val code: String
)

