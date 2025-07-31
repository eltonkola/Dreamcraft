package com.eltonkola.dreamcraft.remote.data.services

import com.eltonkola.dreamcraft.core.ProjectConfig
import com.eltonkola.dreamcraft.remote.data.AiApiService
import com.eltonkola.dreamcraft.remote.data.AiResponse
import com.eltonkola.dreamcraft.remote.data.toAiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

//================================================================
// Claude (Anthropic) API Service
//================================================================
class ClaudeApiService(
    private val client: OkHttpClient,
    private val apiKey: String
) : AiApiService {

    private val apiUrl = "https://api.anthropic.com/v1/messages"

    override suspend fun generate(prompt: String, config: ProjectConfig): AiResponse = withContext(
        Dispatchers.IO) {
        val promptCopy = config.promptTemplate.replace("____", prompt)

        val requestBody = ClaudeRequest(
            model = "claude-3-sonnet-20240229",
            messages = listOf(SharedMessage(role = "user", content = promptCopy)),
            maxTokens = 4096,
            temperature = 0.7
        )

        val jsonBody = json.encodeToString(ClaudeRequest.serializer(), requestBody)
        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBodyString = response.body?.string() ?: ""

        if (!response.isSuccessful) throw IOException("Claude API call failed (${response.code}): $responseBodyString")

        val claudeResponse = json.decodeFromString(ClaudeResponse.serializer(), responseBodyString)
        val content = claudeResponse.content.firstOrNull()?.text ?: throw IOException("No content in Claude response")
        content.toAiResponse()
    }
}
