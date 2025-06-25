package com.eltonkola.dreamcraft.data.remote

import android.content.Context
import com.eltonkola.dreamcraft.core.ProjectConfig
import com.eltonkola.dreamcraft.data.FileManager
import com.eltonkola.dreamcraft.remote.data.AiApiService
import com.eltonkola.dreamcraft.data.AiRepository
import com.eltonkola.dreamcraft.remote.data.AiIntegration
import com.eltonkola.dreamcraft.remote.data.LocalLlmService
import com.eltonkola.dreamcraft.ui.screens.game.editor.FileItem


class AiRepositoryImpl(
    private val groqApiService: AiApiService,
    private val fileManager: FileManager,
    private val context: Context
) : AiRepository {

    override suspend fun generateGame(integration: AiIntegration,
                                      prompt: String,
                                      projectName: String,
                                      config: ProjectConfig,
                                      file: FileItem?): Result<String> {
        return try {
            val response =
                when(integration){
                    is AiIntegration.GROQ -> groqApiService.generateGame(prompt, config)
                    is AiIntegration.LOCAL -> LocalLlmService(integration.llmPath, context).generateGame(prompt, config)
                }

            val filePath = fileManager.saveFile(response.code, projectName, file)
            Result.success(response.thought ?: "Code generated and file updated: $filePath!")
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}