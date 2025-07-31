package com.eltonkola.dreamcraft.remote.data.services

import android.content.Context
import android.util.Log
import com.eltonkola.dreamcraft.core.ProjectConfig
import com.eltonkola.dreamcraft.remote.data.AiApiService
import com.eltonkola.dreamcraft.remote.data.AiResponse
import com.eltonkola.dreamcraft.remote.data.toAiResponse
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
class LocalModelApiService(
    private val context: Context,
    private val modelPath: String
) : AiApiService {

    private val llmInference: LlmInference

    init {
        // Eager initialization with all original options
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(1024)
            //.setTemperature(0.7f)
            .setMaxTopK(64)
            .build()

        llmInference = LlmInference.createFromOptions(context, options)
        Log.i("LocalModelApiService", "LLM initialized with maxTokens=1024, topK=64")
    }

    override suspend fun generate(prompt: String, config: ProjectConfig): AiResponse =
        withContext(Dispatchers.IO) {
            val promptCopy = config.promptTemplate.replace("____", prompt)
            try {
                llmInference.generateResponse(promptCopy).toAiResponse()
            } catch (e: Exception) {
                Log.e("LocalModelApiService", "Generation failed", e)
                throw e
            }
        }
}