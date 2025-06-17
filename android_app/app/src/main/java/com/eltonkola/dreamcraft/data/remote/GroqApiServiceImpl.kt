package com.eltonkola.dreamcraft.data.remote

import com.eltonkola.dreamcraft.data.GroqApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException

class GroqApiServiceImpl(
    private val client: OkHttpClient,
    private val apiKey: String
) : GroqApiService {

    private val apiUrl = "https://api.groq.com/openai/v1/chat/completions"

    override suspend fun generateGame(prompt: String): String = withContext(Dispatchers.IO) {
        val prompt = """
            Create a complete Love2D (LÖVE) game in Lua.
            Game specifications: $prompt.
            Make it a fully playable game with arrow key controls.
            Provide only the Lua code without any explanations or markdown formatting.
        """.trimIndent()

        val requestBody = GroqRequest(
            model = "mixtral-8x7b-32768",
            messages = listOf(
                Message(role = "user", content = prompt)
            ),
            maxTokens = 2048,
            temperature = 0.7
        )

        val json = Json.encodeToString(requestBody)
        val body = RequestBody.create(
            "application/json".toMediaType(),
            json
        )

        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("API call failed: ${response.code}")
        }

        val responseBody = response.body?.string()
            ?: throw IOException("Empty response body")

        val groqResponse = Json.decodeFromString<GroqResponse>(responseBody)
        val luaCode = groqResponse.choices.firstOrNull()?.message?.content
            ?: throw IOException("No content in response")

        // Clean up any potential markdown formatting
        luaCode.replace(Regex("```(?:lua)?\\n?"), "").trim()
    }
}

data class GroqRequest(
    val model: String,
    val messages: List<Message>,
    val maxTokens: Int,
    val temperature: Double
)


data class Message(
    val role: String,
    val content: String
)
data class GroqResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)



