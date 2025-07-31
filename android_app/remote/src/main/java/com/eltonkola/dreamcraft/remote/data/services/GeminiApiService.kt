package com.eltonkola.dreamcraft.remote.data.services

import android.util.Log
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
// Gemini (Google Cloud) API Service
//================================================================
class GeminiApiService(
    private val client: OkHttpClient,
    private val apiKey: String
) : AiApiService {

    private val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-latest:generateContent"

    override suspend fun generate(prompt: String, config: ProjectConfig): AiResponse = withContext(
        Dispatchers.IO) {
        val promptCopy = config.promptTemplate.replace("____", prompt)

        val requestBody = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = promptCopy))))
        )

        val jsonBody = json.encodeToString(GeminiRequest.serializer(), requestBody)
        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBodyString = response.body?.string() ?: ""

        Log.d("GeminiApi", "Response Body: $responseBodyString")
        if (!response.isSuccessful) throw IOException("Gemini API call failed (${response.code}): $responseBodyString")

        val geminiResponse = json.decodeFromString(GeminiResponse.serializer(), responseBodyString)
        val content = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IOException("No content in Gemini response or content was blocked")
        content.toAiResponse()
    }
}