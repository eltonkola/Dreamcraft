package com.eltonkola.dreamcraft.remote.data.services

import android.content.Context
import android.util.Log
import com.eltonkola.dreamcraft.core.ProjectConfig
import com.eltonkola.dreamcraft.remote.data.AiApiService
import com.eltonkola.dreamcraft.remote.data.AiResponse
import com.eltonkola.dreamcraft.remote.data.toAiResponse
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
class LocalModelApiService(
    private val context: Context,
    private val modelPath: String
) : AiApiService {


    private val coroutineScope = CoroutineScope(Dispatchers.IO) // Background scope
    private val llmInference: Deferred<LlmInference> = coroutineScope.async {
        // Runs in background
        Log.d("LocalModel", "Loading model in background...")
        LlmInference.createFromOptions(
            context,
            LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(2048)
                .setMaxTopK(40)
                .build()
        )
    }

    override suspend fun generate(prompt: String, config: ProjectConfig): AiResponse {
        val loadedModel = llmInference.await() // Suspends until model is ready
        val promptCopy = config.promptTemplate.replace("____", prompt)

        return withContext(Dispatchers.IO) {
            try {
                loadedModel.generateResponse(promptCopy).toAiResponse()
            } catch (e: Exception) {
                Log.e("LocalModelApiService", "Generation failed", e)
                throw e
            }
        }
    }

}