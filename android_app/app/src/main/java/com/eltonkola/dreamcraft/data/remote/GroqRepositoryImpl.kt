package com.eltonkola.dreamcraft.data.remote

import com.eltonkola.dreamcraft.data.FileManager
import com.eltonkola.dreamcraft.data.GroqApiService
import com.eltonkola.dreamcraft.data.GroqRepository


class GroqRepositoryImpl(
    private val apiService: GroqApiService,
    private val fileManager: FileManager
) : GroqRepository {

    override suspend fun generateGame(prompt: String, projectName: String): Result<String> {
        return try {
            val luaCode = apiService.generateGame(prompt)
            val filePath = fileManager.saveLuaFile(luaCode, projectName)
            Result.success(filePath)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}