package com.eltonkola.dreamcraft.data.remote

import com.eltonkola.dreamcraft.core.ProjectConfig
import com.eltonkola.dreamcraft.core.data.FileManager
import com.eltonkola.dreamcraft.core.model.FileItem
import com.eltonkola.dreamcraft.data.AiRepository
import com.eltonkola.dreamcraft.remote.data.ActiveAiConfig
import com.eltonkola.dreamcraft.remote.data.AiApiServiceFactory
import com.eltonkola.dreamcraft.remote.data.AiResponse

class AiRepositoryImpl(
    private val  aiApiServiceFactory: AiApiServiceFactory,
    private val fileManager: FileManager
) : AiRepository {

    override suspend fun generateGame(aiConfig: ActiveAiConfig,
                                      prompt: String,
                                      projectName: String,
                                      config: ProjectConfig,
                                      file: FileItem?): Result<AiResponse> {
        return try {
            val service = aiApiServiceFactory.create(aiConfig)
            val response = service?.generate(prompt, config)


            if(response !=null){
                fileManager.saveFile(response.code, projectName, file)

                Result.success(response)
            }else{
                Result.failure(Exception("Cant create the ai config"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}