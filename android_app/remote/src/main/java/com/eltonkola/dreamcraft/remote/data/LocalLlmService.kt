package com.eltonkola.dreamcraft.remote.data

import android.content.Context
import android.util.Log
import com.eltonkola.dreamcraft.core.ProjectConfig
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalLlmService(
    val modelPath: String,
    val context: Context
) : AiApiService {


   private val taskOptions : LlmInference.LlmInferenceOptions by lazy {

              LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTopK(64)
                    .build()
   }

    private val llmInference : LlmInference  by lazy {
        LlmInference.createFromOptions(context, taskOptions)
    }

    override suspend fun generateGame(prompt: String, config: ProjectConfig): AiResponse = withContext(Dispatchers.IO) {

        val promptCopy = config.promptTemplate.replace("____", prompt)

        try {
            val result = llmInference.generateResponse(promptCopy)
            Log.i("", "result: $result")
            result.toAiResponse()

        } catch (e: Exception) {
            Log.e("LocalLlmService", "Error generating response", e)
            throw e
        }
    }

}