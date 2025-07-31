package com.eltonkola.dreamcraft.remote.data.services


import android.util.Log
import com.eltonkola.dreamcraft.core.ProjectConfig
import com.eltonkola.dreamcraft.remote.data.AiApiService
import com.eltonkola.dreamcraft.remote.data.AiResponse
import com.eltonkola.dreamcraft.remote.data.toAiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException



//================================================================
// OpenAI API Service
//================================================================
class OpenAiApiService(
    private val client: OkHttpClient,
    private val apiKey: String
) : AiApiService {
    private val json = Json { ignoreUnknownKeys = true }
    private val apiUrl = "https://api.openai.com/v1/chat/completions"

    override suspend fun generate(prompt: String, config: ProjectConfig): AiResponse = withContext(Dispatchers.IO) {
        val promptCopy = config.promptTemplate.replace("____", prompt)

        val requestBody = OpenAiRequest(
            model = "gpt-4o",
            messages = listOf(SharedMessage(role = "user", content = promptCopy)),
            maxTokens = 4096,
            temperature = 0.7
        )

        val jsonBody = json.encodeToString(OpenAiRequest.serializer(), requestBody)
        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBodyString = response.body?.string() ?: ""

        if (!response.isSuccessful) throw IOException("OpenAI API call failed (${response.code}): $responseBodyString")

        val openAiResponse = json.decodeFromString(OpenAiResponse.serializer(), responseBodyString)
        val content = openAiResponse.choices.firstOrNull()?.message?.content ?: throw IOException("No content in OpenAI response")
        content.toAiResponse()
    }
}
