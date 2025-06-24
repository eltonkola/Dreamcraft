package com.eltonkola.dreamcraft.data

interface GroqApiService {
    suspend fun generateGame(prompt: String): AiResponse
}
