package com.eltonkola.dreamcraft.data.remote

import android.util.Log
import com.eltonkola.dreamcraft.data.GroqApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class GroqApiServiceImpl(
    private val client: OkHttpClient,
    private val apiKey: String
) : GroqApiService {


    private val apiUrl = "https://api.groq.com/openai/v1/chat/completions"
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }


    override suspend fun generateGame(prompt: String): String = withContext(Dispatchers.IO) {
        val prompt = """
            Create a complete Love2D (LÖVE) game in Lua.
            Game specifications: $prompt.
            Make it a fully playable game with arrow key controls.
            Provide only the Lua code without any explanations or markdown formatting.
        """.trimIndent()

        val requestBody = GroqRequest(
            model = "deepseek-r1-distill-llama-70b",
            messages = listOf(
                GroqMessage(role = "user", content = prompt)
            ),
            maxTokens = 2048,
            temperature = 0.7
        )

        val jsonBody = json.encodeToString(requestBody)
        Log.d("GroqAPI", "Request: $jsonBody")

        val body = jsonBody
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBodyString = response.body?.string() ?: ""

        Log.d("GroqAPI", "Response code: ${response.code}")
        Log.d("GroqAPI", "Response: $responseBodyString")

        if (!response.isSuccessful) {
            try {
                val errorResponse = json.decodeFromString<GroqError>(responseBodyString)
                throw IOException("Groq API Error: ${errorResponse.error.message}")
            } catch (e: SerializationException) {
                throw IOException("API call failed (${response.code}): $responseBodyString")
            }
        }

        try {
            val groqResponse = json.decodeFromString<GroqResponse>(responseBodyString)
            val content = groqResponse.choices.firstOrNull()?.message?.content
                ?: throw IOException("No content in response")

            // Clean up markdown formatting
            content.replace(Regex("```(?:lua)?\\n?"), "").trim()

        } catch (e: SerializationException) {
            Log.e("GroqAPI", "Serialization error", e)
            throw IOException("Failed to parse response: ${e.message}")
        }
    }
}
@Serializable
data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>,
    @SerialName("max_tokens") val maxTokens: Int,
    val temperature: Double,
    @SerialName("top_p") val topP: Double = 1.0,
    val stream: Boolean = false
)

@Serializable
data class GroqMessage(
    val role: String,
    val content: String
)

@Serializable
data class GroqResponse(
    val choices: List<GroqChoice>
)

@Serializable
data class GroqChoice(
    val message: GroqMessage,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class GroqError(
    val error: GroqErrorDetail
)

@Serializable
data class GroqErrorDetail(
    val message: String,
    val type: String? = null,
    val code: String? = null
)



