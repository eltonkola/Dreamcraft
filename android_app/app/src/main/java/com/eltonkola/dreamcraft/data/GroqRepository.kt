package com.eltonkola.dreamcraft.data

interface GroqRepository {
    suspend fun generateGame(prompt: String, projectName: String): Result<String>
}