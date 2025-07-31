package com.eltonkola.dreamcraft.remote.data.services

import android.util.Log
import com.eltonkola.dreamcraft.core.ProjectConfig
import com.eltonkola.dreamcraft.remote.data.AiApiService
import com.eltonkola.dreamcraft.remote.data.AiResponse
import com.eltonkola.dreamcraft.remote.data.toAiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlin.String

/**
 * The "engine" for making API calls to the Groq service.
 * It implements the shared AiApiService interface.
 *
 * @param client The shared OkHttpClient instance.
 * @param apiKey The user-provided API key for Groq.
 */
class GroqApiService(
    private val client: OkHttpClient,
    private val apiKey: String
) : AiApiService {

    private val apiUrl = "https://api.groq.com/openai/v1/chat/completions"
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generate(prompt: String, config: ProjectConfig): AiResponse = withContext(Dispatchers.IO) {
        val promptCopy = config.promptTemplate.replace("____", prompt)

        // Create the request body using the SharedMessage class
        val requestBody = GroqRequest(
            model = "llama3-70b-8192", // A powerful and commonly used model on Groq
            messages = listOf(SharedMessage(role = "user", content = promptCopy)),
            maxTokens = 4096,
            temperature = 0.7
        )

        val jsonBody = json.encodeToString(GroqRequest.serializer(), requestBody)
        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBodyString = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            // It's good practice to try and parse a structured error from the API
            try {
                val errorResponse = json.decodeFromString<GroqError>(responseBodyString)
                throw IOException("Groq API Error: ${errorResponse.error.message}")
            } catch (e: Exception) {
                throw IOException("Groq API call failed (${response.code}): $responseBodyString")
            }
        }

        try {
            val groqResponse = json.decodeFromString(GroqResponse.serializer(), responseBodyString)
            val content = groqResponse.choices.firstOrNull()?.message?.content
                ?: throw IOException("No content in Groq response")

            // The 'toAiResponse()' extension function will handle parsing the final JSON
            content.toAiResponse()
        } catch (e: Exception) {
            throw IOException("Failed to parse Groq response: ${e.message}")
        }
    }
}


//============================================================
// Data Classes for Groq API Serialization
//============================================================

@Serializable
data class GroqRequest(
    val model: String,
    val messages: List<SharedMessage>, // Uses the common SharedMessage class
    @SerialName("max_tokens") val maxTokens: Int,
    val temperature: Double,
    @SerialName("top_p") val topP: Double = 1.0,
    val stream: Boolean = false
)

@Serializable
data class GroqResponse(
    val choices: List<GroqChoice>
)

@Serializable
data class GroqChoice(
    val message: SharedMessage, // Uses the common SharedMessage class
    @SerialName("finish_reason") val finishReason: String? = null
)

// Standard error object for OpenAI-compatible APIs
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
