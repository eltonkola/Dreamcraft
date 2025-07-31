package com.eltonkola.dreamcraft.remote.data.services


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}


// This is the shared Message class used by Groq, OpenAI, and Claude.
@Serializable
data class SharedMessage(
    val role: String,
    val content: String
)

//================================================================
// OpenAI Data Classes (Identical structure to Groq's)
//================================================================
@Serializable
data class OpenAiRequest(
    val model: String,
    val messages: List<SharedMessage>,
    @SerialName("max_tokens") val maxTokens: Int,
    val temperature: Double
)

@Serializable
data class OpenAiResponse(
    val choices: List<GroqChoice> // We can reuse Groq's Choice/Message classes
)


//================================================================
// Claude (Anthropic) Data Classes
//================================================================
@Serializable
data class ClaudeRequest(
    val model: String,
    val messages: List<SharedMessage>,
    @SerialName("max_tokens") val maxTokens: Int,
    val temperature: Double
)

@Serializable
data class ClaudeResponse(
    val content: List<ClaudeContentBlock>
)

@Serializable
data class ClaudeContentBlock(
    val text: String
)


//================================================================
// Gemini (Google) Data Classes
//================================================================
@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent?,
    @SerialName("finishReason") val finishReason: String?,
    val safetyRatings: List<GeminiSafetyRating>?
)

@Serializable
data class GeminiSafetyRating(
    val category: String,
    val probability: String
)