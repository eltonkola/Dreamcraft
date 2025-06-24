package com.eltonkola.dreamcraft.remote

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalLlmService(
    val modelPath: String,
    val context: Context
) : AiApiService {


   private val taskOptions = LlmInference.LlmInferenceOptions.builder()
        .setModelPath(modelPath)
        .setMaxTopK(64)
        .build()

    private val llmInference = LlmInference.createFromOptions(context, taskOptions)

    override suspend fun generateGame(prompt: String): AiResponse = withContext(Dispatchers.IO) {

        val promptCopy = """
You are a Lua code generator.
Your task is to create a complete, playable Love2D (LÖVE) game in Lua.

Requirements:
- The game must be fully playable.
- It must use arrow key controls.
- It must be a single Lua source file with the complete code.
- Do not include any explanation, comments, or markdown.
- Output only plain Lua source code. Do not include ``` or any descriptive text.

Game idea: $prompt
""".trimIndent()

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