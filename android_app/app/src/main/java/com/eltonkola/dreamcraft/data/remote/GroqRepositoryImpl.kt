package com.eltonkola.dreamcraft.data.remote

import com.eltonkola.dreamcraft.data.FileManager
import com.eltonkola.dreamcraft.data.GroqApiService
import com.eltonkola.dreamcraft.data.GroqRepository
import com.eltonkola.dreamcraft.ui.screens.game.editor.FileItem


class GroqRepositoryImpl(
    private val apiService: GroqApiService,
    private val fileManager: FileManager
) : GroqRepository {

    override suspend fun generateGame(prompt: String, projectName: String, file: FileItem?): Result<String> {
        return try {
            val response = apiService.generateGame(prompt)
            val filePath = fileManager.saveLuaFile(response.code, projectName, file)
            Result.success(response.thought ?: "Code generated and file updated: $filePath!")
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}