package com.eltonkola.dreamcraft.remote

import android.content.Context
import android.net.http.HttpResponseCache.install
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.slf4j.MDC.put
import java.io.File

// Enhanced data classes for model management
data class LocalModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val size: Long, // in bytes
    val downloadUrl: String,
    val filename: String,
    val version: String,
    val requiredRam: Long, // in MB
    val supportedLanguages: List<String> = listOf("lua")
)

data class ModelDownloadProgress(
    val modelId: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val isComplete: Boolean,
    val error: String? = null
) {
    val progressPercent: Float get() = if (totalBytes > 0) (bytesDownloaded.toFloat() / totalBytes * 100) else 0f
}

data class AIServiceStatus(
    val provider: AIProvider,
    val isConfigured: Boolean,
    val isAvailable: Boolean,
    val lastTestedAt: Long? = null,
    val testResult: String? = null,
    val config: AIConfig? = null
)

// Core interfaces and data classes
data class CodeGenerationRequest(
    val prompt: String,
    val context: String? = null,
    val maxTokens: Int = 1000,
    val temperature: Float = 0.7f
)

data class CodeGenerationResponse(
    val generatedCode: String,
    val success: Boolean,
    val errorMessage: String? = null,
    val tokensUsed: Int? = null
)

// AI Provider Types
sealed class AIProvider(val displayName: String, val requiresApiKey: Boolean, val supportsLocalModels: Boolean) {
    object LocalLLM : AIProvider("Local LLM", false, true)
    object ChatGPT : AIProvider("ChatGPT", true, false)
    object Claude : AIProvider("Claude", true, false)
    object Ollama : AIProvider("Ollama", false, false)
    object Groq : AIProvider("Groq", true, false)
}

// Configuration for each provider
sealed class AIConfig {
    data class LocalLLMConfig(
        val modelId: String,
        val modelPath: String,
        val maxThreads: Int = 4,
        val contextLength: Int = 2048
    ) : AIConfig()

    data class ChatGPTConfig(
        val apiKey: String,
        val model: String = "gpt-3.5-turbo",
        val baseUrl: String = "https://api.openai.com/v1",
        val isValid: Boolean = false,
        val lastValidated: Long? = null
    ) : AIConfig()

    data class ClaudeConfig(
        val apiKey: String,
        val model: String = "claude-3-haiku-20240307",
        val baseUrl: String = "https://api.anthropic.com/v1",
        val isValid: Boolean = false,
        val lastValidated: Long? = null
    ) : AIConfig()

    data class OllamaConfig(
        val baseUrl: String = "http://localhost:11434",
        val model: String,
        val isConnected: Boolean = false,
        val lastChecked: Long? = null
    ) : AIConfig()

    data class GroqConfig(
        val apiKey: String,
        val model: String = "mixtral-8x7b-32768",
        val baseUrl: String = "https://api.groq.com/openai/v1",
        val isValid: Boolean = false,
        val lastValidated: Long? = null
    ) : AIConfig()
}

// Model Download Manager
interface ModelDownloadManager {
    suspend fun getAvailableModels(): List<LocalModelInfo>
    suspend fun downloadModel(modelInfo: LocalModelInfo): Flow<ModelDownloadProgress>
    suspend fun getDownloadedModels(): List<LocalModelInfo>
    suspend fun deleteModel(modelId: String): Boolean
    suspend fun isModelDownloaded(modelId: String): Boolean
    suspend fun getModelPath(modelId: String): String?
    suspend fun cancelDownload(modelId: String)
}

class ModelDownloadManagerImpl(
    private val context: Context,
    private val httpClient: HttpClient
) : ModelDownloadManager {

    private val modelsDir = File(context.filesDir, "ai_models")
    private val activeDownloads = mutableMapOf<String, Job>()

    init {
        modelsDir.mkdirs()
    }

    override suspend fun getAvailableModels(): List<LocalModelInfo> {
        // This would typically fetch from a remote API or embedded list
        return listOf(
            LocalModelInfo(
                id = "phi-3-mini",
                name = "Phi-3 Mini",
                description = "Microsoft's small but capable model, good for code generation",
                size = 2_300_000_000L, // ~2.3GB
                downloadUrl = "https://example.com/models/phi-3-mini.gguf",
                filename = "phi-3-mini.gguf",
                version = "1.0",
                requiredRam = 4096
            ),
            LocalModelInfo(
                id = "codellama-7b",
                name = "CodeLlama 7B",
                description = "Meta's code-specialized model",
                size = 3_800_000_000L, // ~3.8GB
                downloadUrl = "https://example.com/models/codellama-7b.gguf",
                filename = "codellama-7b.gguf",
                version = "1.0",
                requiredRam = 8192
            ),
            LocalModelInfo(
                id = "deepseek-coder-1.3b",
                name = "DeepSeek Coder 1.3B",
                description = "Lightweight coding model",
                size = 800_000_000L, // ~800MB
                downloadUrl = "https://example.com/models/deepseek-coder-1.3b.gguf",
                filename = "deepseek-coder-1.3b.gguf",
                version = "1.0",
                requiredRam = 2048
            )
        )
    }

    override suspend fun downloadModel(modelInfo: LocalModelInfo): Flow<ModelDownloadProgress> = flow {
        val modelFile = File(modelsDir, modelInfo.filename)

        if (modelFile.exists()) {
            emit(ModelDownloadProgress(modelInfo.id, modelInfo.size, modelInfo.size, true))
            return@flow
        }

        try {
            val response = httpClient.get(modelInfo.downloadUrl) {
                onDownload { bytesSentTotal, contentLength ->
                    // This callback might not work with all HTTP clients
                    // You may need to implement chunked reading
                }
            }

            val channel = response.bodyAsChannel()
            val buffer = ByteArray(8192)
            var bytesDownloaded = 0L

            modelFile.outputStream().use { output ->
                while (!channel.isClosedForRead) {
                    val bytes = channel.readAvailable(buffer)
                    if (bytes > 0) {
                        output.write(buffer, 0, bytes)
                        bytesDownloaded += bytes

                        emit(ModelDownloadProgress(
                            modelInfo.id,
                            bytesDownloaded,
                            modelInfo.size,
                            false
                        ))
                    }
                }
            }

            emit(ModelDownloadProgress(modelInfo.id, bytesDownloaded, modelInfo.size, true))

        } catch (e: Exception) {
            emit(ModelDownloadProgress(modelInfo.id, 0, modelInfo.size, false, e.message))
        }
    }

    override suspend fun getDownloadedModels(): List<LocalModelInfo> {
        val availableModels = getAvailableModels()
        return availableModels.filter { isModelDownloaded(it.id) }
    }

    override suspend fun deleteModel(modelId: String): Boolean {
        val availableModels = getAvailableModels()
        val modelInfo = availableModels.find { it.id == modelId } ?: return false
        val modelFile = File(modelsDir, modelInfo.filename)
        return modelFile.delete()
    }

    override suspend fun isModelDownloaded(modelId: String): Boolean {
        val availableModels = getAvailableModels()
        val modelInfo = availableModels.find { it.id == modelId } ?: return false
        val modelFile = File(modelsDir, modelInfo.filename)
        return modelFile.exists() && modelFile.length() == modelInfo.size
    }

    override suspend fun getModelPath(modelId: String): String? {
        if (!isModelDownloaded(modelId)) return null
        val availableModels = getAvailableModels()
        val modelInfo = availableModels.find { it.id == modelId } ?: return null
        return File(modelsDir, modelInfo.filename).absolutePath
    }

    override suspend fun cancelDownload(modelId: String) {
        activeDownloads[modelId]?.cancel()
        activeDownloads.remove(modelId)
    }
}

// API Key Validator
interface APIKeyValidator {
    suspend fun validateChatGPT(config: AIConfig.ChatGPTConfig): Boolean
    suspend fun validateClaude(config: AIConfig.ClaudeConfig): Boolean
    suspend fun validateGroq(config: AIConfig.GroqConfig): Boolean
    suspend fun checkOllama(config: AIConfig.OllamaConfig): Boolean
}

class APIKeyValidatorImpl(private val httpClient: HttpClient) : APIKeyValidator {

    override suspend fun validateChatGPT(config: AIConfig.ChatGPTConfig): Boolean {
        return try {
            val response = httpClient.get("${config.baseUrl}/models") {
                header("Authorization", "Bearer ${config.apiKey}")
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun validateClaude(config: AIConfig.ClaudeConfig): Boolean {
        return try {
            val testRequest = buildJsonObject {
                put("model", config.model)
//                put("max_tokens", 10)
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", "test")
                    })
                })
            }

            val response = httpClient.post("${config.baseUrl}/messages") {
                header("x-api-key", config.apiKey)
                header("Content-Type", "application/json")
                header("anthropic-version", "2023-06-01")
                setBody(testRequest.toString())
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun validateGroq(config: AIConfig.GroqConfig): Boolean {
        return try {
            val response = httpClient.get("${config.baseUrl}/models") {
                header("Authorization", "Bearer ${config.apiKey}")
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun checkOllama(config: AIConfig.OllamaConfig): Boolean {
        return try {
            val response = httpClient.get("${config.baseUrl}/api/tags")
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }
}

// Enhanced AI Service Management
class AIServiceManager(
    private val modelDownloadManager: ModelDownloadManager,
    private val apiKeyValidator: APIKeyValidator,
    private val configRepository: AIConfigRepository,
//    private val codeGeneratorFactory: AICodeGeneratorFactory
) {
    private val _serviceStatuses = MutableStateFlow<List<AIServiceStatus>>(emptyList())
    val serviceStatuses: StateFlow<List<AIServiceStatus>> = _serviceStatuses.asStateFlow()

    private var activeGenerator: AICodeGenerator? = null
    private var activeProvider: AIProvider? = null

    suspend fun initializeServices() {
        refreshServiceStatuses()
    }

    suspend fun addLocalModel(modelInfo: LocalModelInfo): Flow<ModelDownloadProgress> {
        return modelDownloadManager.downloadModel(modelInfo).onCompletion {
            refreshServiceStatuses()
        }
    }

    suspend fun addOnlineService(provider: AIProvider, config: AIConfig): Boolean {
        val isValid = when (provider) {
            AIProvider.ChatGPT -> apiKeyValidator.validateChatGPT(config as AIConfig.ChatGPTConfig)
            AIProvider.Claude -> apiKeyValidator.validateClaude(config as AIConfig.ClaudeConfig)
            AIProvider.Groq -> apiKeyValidator.validateGroq(config as AIConfig.GroqConfig)
            AIProvider.Ollama -> apiKeyValidator.checkOllama(config as AIConfig.OllamaConfig)
            AIProvider.LocalLLM -> true // Handled separately
        }

        if (isValid) {
            val updatedConfig = when (config) {
                is AIConfig.ChatGPTConfig -> config.copy(isValid = true, lastValidated = System.currentTimeMillis())
                is AIConfig.ClaudeConfig -> config.copy(isValid = true, lastValidated = System.currentTimeMillis())
                is AIConfig.GroqConfig -> config.copy(isValid = true, lastValidated = System.currentTimeMillis())
                is AIConfig.OllamaConfig -> config.copy(isConnected = true, lastChecked = System.currentTimeMillis())
                else -> config
            }
            configRepository.saveConfig(provider, updatedConfig)
            refreshServiceStatuses()
        }

        return isValid
    }

    suspend fun removeService(provider: AIProvider) {
        when (provider) {
            AIProvider.LocalLLM -> {
                // Show dialog to select which model to remove
                val downloadedModels = modelDownloadManager.getDownloadedModels()
                // Implementation would show selection dialog
            }
            else -> {
                configRepository.deleteConfig(provider)
                refreshServiceStatuses()
            }
        }
    }

    suspend fun setActiveService(provider: AIProvider, modelId: String? = null): Boolean {
        val config = configRepository.getConfig(provider)
        if (config == null) return false

        val finalConfig = if (provider == AIProvider.LocalLLM && modelId != null) {
            val modelPath = modelDownloadManager.getModelPath(modelId)
            if (modelPath == null) return false
            AIConfig.LocalLLMConfig(modelId, modelPath)
        } else {
            config
        }

//        activeGenerator = codeGeneratorFactory.create(provider, finalConfig)
        activeProvider = provider

        return activeGenerator?.isAvailable() == true
    }

    suspend fun generateCode(request: CodeGenerationRequest): CodeGenerationResponse {
        return activeGenerator?.generateCode(request)
            ?: CodeGenerationResponse("", false, "No active AI service selected")
    }

    suspend fun getAvailableLocalModels(): List<LocalModelInfo> {
        return modelDownloadManager.getAvailableModels()
    }

    suspend fun getDownloadedModels(): List<LocalModelInfo> {
        return modelDownloadManager.getDownloadedModels()
    }

    fun getActiveProvider(): AIProvider? = activeProvider

    private suspend fun refreshServiceStatuses() {
        val statuses = mutableListOf<AIServiceStatus>()

        // Check local models
        val downloadedModels = modelDownloadManager.getDownloadedModels()
        if (downloadedModels.isNotEmpty()) {
            statuses.add(AIServiceStatus(
                provider = AIProvider.LocalLLM,
                isConfigured = true,
                isAvailable = true,
                config = null // Multiple models available
            ))
        }

        // Check online services
        val allConfigs = configRepository.getAllConfigs()
        for ((provider, config) in allConfigs) {
            if (provider != AIProvider.LocalLLM) {
                val isAvailable = when (config) {
                    is AIConfig.ChatGPTConfig -> config.isValid
                    is AIConfig.ClaudeConfig -> config.isValid
                    is AIConfig.GroqConfig -> config.isValid
                    is AIConfig.OllamaConfig -> config.isConnected
                    else -> false
                }

                statuses.add(AIServiceStatus(
                    provider = provider,
                    isConfigured = true,
                    isAvailable = isAvailable,
                    config = config
                ))
            }
        }

        _serviceStatuses.value = statuses
    }

    suspend fun refreshServiceStatus(provider: AIProvider) {
        val config = configRepository.getConfig(provider) ?: return

        val isValid = when (provider) {
            AIProvider.ChatGPT -> apiKeyValidator.validateChatGPT(config as AIConfig.ChatGPTConfig)
            AIProvider.Claude -> apiKeyValidator.validateClaude(config as AIConfig.ClaudeConfig)
            AIProvider.Groq -> apiKeyValidator.validateGroq(config as AIConfig.GroqConfig)
            AIProvider.Ollama -> apiKeyValidator.checkOllama(config as AIConfig.OllamaConfig)
            AIProvider.LocalLLM -> true
        }

        if (isValid) {
            val updatedConfig = when (config) {
                is AIConfig.ChatGPTConfig -> config.copy(isValid = true, lastValidated = System.currentTimeMillis())
                is AIConfig.ClaudeConfig -> config.copy(isValid = true, lastValidated = System.currentTimeMillis())
                is AIConfig.GroqConfig -> config.copy(isValid = true, lastValidated = System.currentTimeMillis())
                is AIConfig.OllamaConfig -> config.copy(isConnected = true, lastChecked = System.currentTimeMillis())
                else -> config
            }
            configRepository.saveConfig(provider, updatedConfig)
        }

        refreshServiceStatuses()
    }
}

// Enhanced Configuration Repository
interface AIConfigRepository {
    suspend fun getConfig(provider: AIProvider): AIConfig?
    suspend fun saveConfig(provider: AIProvider, config: AIConfig)
    suspend fun deleteConfig(provider: AIProvider)
    suspend fun getAllConfigs(): Map<AIProvider, AIConfig>
}

// Main AI Service Interface (unchanged)
interface AICodeGenerator {
    suspend fun generateCode(request: CodeGenerationRequest): CodeGenerationResponse
    fun isAvailable(): Boolean
    fun getProvider(): AIProvider
}

// Updated Factory
//class AICodeGeneratorFactory(private val httpClient: HttpClient) {
//
//    fun create(provider: AIProvider, config: AIConfig): AICodeGenerator {
//        return when (provider) {
//            AIProvider.LocalLLM -> LocalLLMCodeGenerator(config as AIConfig.LocalLLMConfig)
//            AIProvider.ChatGPT -> ChatGPTCodeGenerator(config as AIConfig.ChatGPTConfig, httpClient)
//            AIProvider.Claude -> ClaudeCodeGenerator(config as AIConfig.ClaudeConfig, httpClient)
//            AIProvider.Ollama -> OllamaCodeGenerator(config as AIConfig.OllamaConfig, httpClient)
//            AIProvider.Groq -> GroqCodeGenerator(config as AIConfig.GroqConfig, httpClient)
//        }
//    }
//}



// Abstract base class for HTTP-based providers
abstract class HttpAICodeGenerator(
    protected val config: AIConfig,
    protected val httpClient: HttpClient
) : AICodeGenerator {

    protected abstract suspend fun makeRequest(request: CodeGenerationRequest): CodeGenerationResponse

    override suspend fun generateCode(request: CodeGenerationRequest): CodeGenerationResponse {
        return try {
            if (!isAvailable()) {
                return CodeGenerationResponse("", false, "AI service not available")
            }
            makeRequest(request)
        } catch (e: Exception) {
            CodeGenerationResponse("", false, "Error: ${e.message}")
        }
    }
}

//class ChatGPTCodeGenerator(
//    config: AIConfig.ChatGPTConfig,
//    httpClient: HttpClient
//) : HttpAICodeGenerator(config, httpClient) {
//
//    private val chatGPTConfig = config as AIConfig.ChatGPTConfig
//
//    override suspend fun makeRequest(request: CodeGenerationRequest): CodeGenerationResponse {
//        return withContext(Dispatchers.IO) {
//            try {
//                val requestBody = buildJsonObject {
//                    put("model", chatGPTConfig.model)
//                    put("messages", buildJsonArray {
//                        add(buildJsonObject {
//                            put("role", "system")
//                            put("content", "You are a professional Lua code generator for Android games. Generate clean, efficient, and well-commented Lua code based on user requirements. Focus on game development patterns and best practices.")
//                        })
//                        if (request.context != null) {
//                            add(buildJsonObject {
//                                put("role", "user")
//                                put("content", "Context: ${request.context}")
//                            })
//                        }
//                        add(buildJsonObject {
//                            put("role", "user")
//                            put("content", request.prompt)
//                        })
//                    })
//                    put("max_tokens", request.maxTokens)
//                    put("temperature", request.temperature)
//                    put("top_p", 1.0)
//                    put("frequency_penalty", 0.0)
//                    put("presence_penalty", 0.0)
//                }
//
//                val response = httpClient.post("${chatGPTConfig.baseUrl}/chat/completions") {
//                    header("Authorization", "Bearer ${chatGPTConfig.apiKey}")
//                    header("Content-Type", "application/json")
//                    setBody(requestBody.toString())
//                }
//
//                if (response.status.isSuccess()) {
//                    val responseText = response.bodyAsText()
//                    val jsonResponse = Json.parseToJsonElement(responseText).jsonObject
//
//                    val choices = jsonResponse["choices"]?.jsonArray
//                    if (choices != null && choices.size > 0) {
//                        val content = choices[0].jsonObject["message"]?.jsonObject
//                            ?.get("content")?.jsonPrimitive?.content ?: ""
//                        val tokensUsed = jsonResponse["usage"]?.jsonObject
//                            ?.get("total_tokens")?.jsonPrimitive?.int
//
//                        val cleanedCode = extractLuaCode(content)
//                        CodeGenerationResponse(cleanedCode, true, tokensUsed = tokensUsed)
//                    } else {
//                        CodeGenerationResponse("", false, "No response from ChatGPT")
//                    }
//                } else {
//                    val errorBody = response.bodyAsText()
//                    CodeGenerationResponse("", false, "ChatGPT API error (${response.status}): $errorBody")
//                }
//            } catch (e: Exception) {
//                CodeGenerationResponse("", false, "ChatGPT request failed: ${e.message}")
//            }
//        }
//    }
//
//    override fun isAvailable(): Boolean = chatGPTConfig.apiKey.isNotBlank() && chatGPTConfig.isValid
//    override fun getProvider(): AIProvider = AIProvider.ChatGPT
//}
//
//class ClaudeCodeGenerator(
//    config: AIConfig.ClaudeConfig,
//    httpClient: HttpClient
//) : HttpAICodeGenerator(config, httpClient) {
//
//    private val claudeConfig = config as AIConfig.ClaudeConfig
//
//    override suspend fun makeRequest(request: CodeGenerationRequest): CodeGenerationResponse {
//        return withContext(Dispatchers.IO) {
//            try {
//                val systemPrompt = "You are a professional Lua code generator specialized in Android game development. Generate clean, efficient, and well-commented Lua code. Focus on performance and best practices for mobile games."
//
//                val userContent = buildString {
//                    if (request.context != null) {
//                        append("Context: ${request.context}\n\n")
//                    }
//                    append("Request: ${request.prompt}")
//                }
//
//                val requestBody = buildJsonObject {
//                    put("model", claudeConfig.model)
//                    put("max_tokens", request.maxTokens)
//                    put("system", systemPrompt)
//                    put("messages", buildJsonArray {
//                        add(buildJsonObject {
//                            put("role", "user")
//                            put("content", userContent)
//                        })
//                    })
//                    put("temperature", request.temperature)
//                }
//
//                val response = httpClient.post("${claudeConfig.baseUrl}/messages") {
//                    header("x-api-key", claudeConfig.apiKey)
//                    header("Content-Type", "application/json")
//                    header("anthropic-version", "2023-06-01")
//                    setBody(requestBody.toString())
//                }
//
//                if (response.status.isSuccess()) {
//                    val responseText = response.bodyAsText()
//                    val jsonResponse = Json.parseToJsonElement(responseText).jsonObject
//
//                    val content = jsonResponse["content"]?.jsonArray?.get(0)?.jsonObject
//                        ?.get("text")?.jsonPrimitive?.content ?: ""
//
//                    val usage = jsonResponse["usage"]?.jsonObject
//                    val tokensUsed = usage?.get("input_tokens")?.jsonPrimitive?.int?.let { input ->
//                        usage["output_tokens"]?.jsonPrimitive?.int?.let { output ->
//                            input + output
//                        }
//                    }
//
//                    val cleanedCode = extractLuaCode(content)
//                    CodeGenerationResponse(cleanedCode, true, tokensUsed = tokensUsed)
//                } else {
//                    val errorBody = response.bodyAsText()
//                    CodeGenerationResponse("", false, "Claude API error (${response.status}): $errorBody")
//                }
//            } catch (e: Exception) {
//                CodeGenerationResponse("", false, "Claude request failed: ${e.message}")
//            }
//        }
//    }
//
//    override fun isAvailable(): Boolean = claudeConfig.apiKey.isNotBlank() && claudeConfig.isValid
//    override fun getProvider(): AIProvider = AIProvider.Claude
//}
//
//class OllamaCodeGenerator(
//    config: AIConfig.OllamaConfig,
//    httpClient: HttpClient
//) : HttpAICodeGenerator(config, httpClient) {
//
//    private val ollamaConfig = config as AIConfig.OllamaConfig
//
//    override suspend fun makeRequest(request: CodeGenerationRequest): CodeGenerationResponse {
//        return withContext(Dispatchers.IO) {
//            try {
//                val systemPrompt = "You are a professional Lua code generator for Android games. Generate clean, efficient, and well-commented Lua code based on user requirements."
//
//                val fullPrompt = buildString {
//                    append("$systemPrompt\n\n")
//                    if (request.context != null) {
//                        append("Context: ${request.context}\n\n")
//                    }
//                    append("Generate Lua code for: ${request.prompt}\n\n")
//                    append("Return only the Lua code without explanations.")
//                }
//
//                val requestBody = buildJsonObject {
//                    put("model", ollamaConfig.model)
//                    put("prompt", fullPrompt)
//                    put("stream", false)
//                    put("options", buildJsonObject {
//                        put("temperature", request.temperature)
//                        put("num_predict", request.maxTokens)
//                        put("top_p", 0.9)
//                        put("repeat_penalty", 1.1)
//                    })
//                }
//
//                val response = httpClient.post("${ollamaConfig.baseUrl}/api/generate") {
//                    header("Content-Type", "application/json")
//                    timeout {
//                        requestTimeoutMillis = 60000 // 60 seconds for local generation
//                    }
//                    setBody(requestBody.toString())
//                }
//
//                if (response.status.isSuccess()) {
//                    val responseText = response.bodyAsText()
//                    val jsonResponse = Json.parseToJsonElement(responseText).jsonObject
//
//                    val content = jsonResponse["response"]?.jsonPrimitive?.content ?: ""
//                    val done = jsonResponse["done"]?.jsonPrimitive?.boolean ?: false
//
//                    if (done && content.isNotEmpty()) {
//                        val cleanedCode = extractLuaCode(content)
//                        CodeGenerationResponse(cleanedCode, true)
//                    } else {
//                        CodeGenerationResponse("", false, "Ollama generation incomplete or empty")
//                    }
//                } else {
//                    val errorBody = response.bodyAsText()
//                    CodeGenerationResponse("", false, "Ollama error (${response.status}): $errorBody")
//                }
//            } catch (e: Exception) {
//                CodeGenerationResponse("", false, "Ollama request failed: ${e.message}")
//            }
//        }
//    }
//
//    override fun isAvailable(): Boolean = ollamaConfig.isConnected
//    override fun getProvider(): AIProvider = AIProvider.Ollama
//
//    // Additional method to check model availability
//    suspend fun isModelAvailable(): Boolean {
//        return try {
//            val response = httpClient.get("${ollamaConfig.baseUrl}/api/tags")
//            if (response.status.isSuccess()) {
//                val responseText = response.bodyAsText()
//                val jsonResponse = Json.parseToJsonElement(responseText).jsonObject
//                val models = jsonResponse["models"]?.jsonArray ?: return false
//
//                models.any { model ->
//                    model.jsonObject["name"]?.jsonPrimitive?.content == ollamaConfig.model
//                }
//            } else {
//                false
//            }
//        } catch (e: Exception) {
//            false
//        }
//    }
//}
//
//class GroqCodeGenerator(
//    config: AIConfig.GroqConfig,
//    httpClient: HttpClient
//) : HttpAICodeGenerator(config, httpClient) {
//
//    private val groqConfig = config as AIConfig.GroqConfig
//
//    override suspend fun makeRequest(request: CodeGenerationRequest): CodeGenerationResponse {
//        return withContext(Dispatchers.IO) {
//            try {
//                val requestBody = buildJsonObject {
//                    put("model", groqConfig.model)
//                    put("messages", buildJsonArray {
//                        add(buildJsonObject {
//                            put("role", "system")
//                            put("content", "You are a professional Lua code generator for Android games. Generate clean, efficient, and well-commented Lua code. Focus on game development patterns and mobile performance optimization.")
//                        })
//                        if (request.context != null) {
//                            add(buildJsonObject {
//                                put("role", "user")
//                                put("content", "Context: ${request.context}")
//                            })
//                        }
//                        add(buildJsonObject {
//                            put("role", "user")
//                            put("content", request.prompt)
//                        })
//                    })
//                    put("max_tokens", request.maxTokens)
//                    put("temperature", request.temperature)
//                    put("top_p", 1.0)
//                    put("stream", false)
//                }
//
//                val response = httpClient.post("${groqConfig.baseUrl}/chat/completions") {
//                    header("Authorization", "Bearer ${groqConfig.apiKey}")
//                    header("Content-Type", "application/json")
//                    timeout {
//                        requestTimeoutMillis = 30000 // Groq is typically fast
//                    }
//                    setBody(requestBody.toString())
//                }
//
//                if (response.status.isSuccess()) {
//                    val responseText = response.bodyAsText()
//                    val jsonResponse = Json.parseToJsonElement(responseText).jsonObject
//
//                    val choices = jsonResponse["choices"]?.jsonArray
//                    if (choices != null && choices.size > 0) {
//                        val content = choices[0].jsonObject["message"]?.jsonObject
//                            ?.get("content")?.jsonPrimitive?.content ?: ""
//
//                        val usage = jsonResponse["usage"]?.jsonObject
//                        val tokensUsed = usage?.get("total_tokens")?.jsonPrimitive?.int
//
//                        val cleanedCode = extractLuaCode(content)
//                        CodeGenerationResponse(cleanedCode, true, tokensUsed = tokensUsed)
//                    } else {
//                        CodeGenerationResponse("", false, "No response from Groq")
//                    }
//                } else {
//                    val errorBody = response.bodyAsText()
//                    val errorMessage = try {
//                        val errorJson = Json.parseToJsonElement(errorBody).jsonObject
//                        errorJson["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
//                            ?: "Unknown error"
//                    } catch (e: Exception) {
//                        errorBody
//                    }
//                    CodeGenerationResponse("", false, "Groq API error (${response.status}): $errorMessage")
//                }
//            } catch (e: Exception) {
//                CodeGenerationResponse("", false, "Groq request failed: ${e.message}")
//            }
//        }
//    }
//
//    override fun isAvailable(): Boolean = groqConfig.apiKey.isNotBlank() && groqConfig.isValid
//    override fun getProvider(): AIProvider = AIProvider.Groq
//}
//
//// Utility function to extract and clean Lua code from AI responses
//private fun extractLuaCode(content: String): String {
//    // Remove common AI response patterns and extract code blocks
//    val codeBlockRegex = Regex("```(?:lua)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
//    val codeMatch = codeBlockRegex.find(content)
//
//    return if (codeMatch != null) {
//        // Found code block, extract it
//        codeMatch.groupValues[1].trim()
//    } else {
//        // No code block found, clean up the entire response
//        content
//            .lines()
//            .dropWhile { line ->
//                // Skip common AI intro phrases
//                line.trim().lowercase().let {
//                    it.startsWith("here") ||
//                            it.startsWith("sure") ||
//                            it.startsWith("i'll") ||
//                            it.startsWith("let me") ||
//                            it.isEmpty()
//                }
//            }
//            .dropLastWhile { line ->
//                // Skip common AI outro phrases
//                line.trim().lowercase().let {
//                    it.startsWith("this code") ||
//                            it.startsWith("the above") ||
//                            it.startsWith("feel free") ||
//                            it.isEmpty()
//                }
//            }
//            .joinToString("\n")
//            .trim()
//    }
//}
//
//// Enhanced error handling for specific API errors
//private fun handleAPIError(statusCode: HttpStatusCode, errorBody: String, provider: String): String {
//    return when (statusCode) {
//        HttpStatusCode.Unauthorized -> "$provider: Invalid API key"
//        HttpStatusCode.TooManyRequests -> "$provider: Rate limit exceeded"
//        HttpStatusCode.BadRequest -> "$provider: Invalid request format"
//        HttpStatusCode.InternalServerError -> "$provider: Server error, try again later"
//        HttpStatusCode.ServiceUnavailable -> "$provider: Service temporarily unavailable"
//        else -> "$provider: HTTP ${statusCode.value} - $errorBody"
//    }
//}
//
//
//class LocalLLMCodeGenerator(
//    private val config: AIConfig.LocalLLMConfig
//) : AICodeGenerator {
//
//    private var llamaContext: Long = 0L // Native context pointer
//    private var isInitialized = false
//
//    init {
//        initializeModel()
//    }
//
//    private fun initializeModel() {
//        try {
//            if (File(config.modelPath).exists()) {
//                llamaContext = nativeInitModel(
//                    config.modelPath,
//                    config.contextLength,
//                    config.maxThreads
//                )
//                isInitialized = llamaContext != 0L
//            }
//        } catch (e: Exception) {
//            Log.e("LocalLLM", "Failed to initialize model: ${e.message}")
//            isInitialized = false
//        }
//    }
//
//    override suspend fun generateCode(request: CodeGenerationRequest): CodeGenerationResponse {
//        return withContext(Dispatchers.IO) {
//            try {
//                if (!isAvailable()) {
//                    return@withContext CodeGenerationResponse(
//                        "", false, "Local LLM not available"
//                    )
//                }
//
//                val systemPrompt = "You are a Lua code generator for Android games. Generate clean, efficient Lua code based on the user's request. Only return the code without explanations."
//                val fullPrompt = buildPrompt(systemPrompt, request.prompt, request.context)
//
//                val result = nativeGenerate(
//                    llamaContext,
//                    fullPrompt,
//                    request.maxTokens,
//                    request.temperature
//                )
//
//                if (result.isNotEmpty()) {
//                    CodeGenerationResponse(result, true)
//                } else {
//                    CodeGenerationResponse("", false, "Failed to generate code")
//                }
//
//            } catch (e: Exception) {
//                CodeGenerationResponse("", false, "Local LLM error: ${e.message}")
//            }
//        }
//    }
//
//    override fun isAvailable(): Boolean {
//        return isInitialized && File(config.modelPath).exists()
//    }
//
//    override fun getProvider(): AIProvider = AIProvider.LocalLLM
//
//    private fun buildPrompt(systemPrompt: String, userPrompt: String, context: String?): String {
//        return buildString {
//            append("<|system|>\n$systemPrompt<|end|>\n")
//            if (context != null) {
//                append("<|context|>\n$context<|end|>\n")
//            }
//            append("<|user|>\n$userPrompt<|end|>\n")
//            append("<|assistant|>\n")
//        }
//    }
//
//    fun cleanup() {
//        if (isInitialized) {
//            nativeCleanup(llamaContext)
//            isInitialized = false
//            llamaContext = 0L
//        }
//    }
//
//    // Native methods - you'll need to implement these with JNI
//    private external fun nativeInitModel(
//        modelPath: String,
//        contextLength: Int,
//        threads: Int
//    ): Long
//
//    private external fun nativeGenerate(
//        context: Long,
//        prompt: String,
//        maxTokens: Int,
//        temperature: Float
//    ): String
//
//    private external fun nativeCleanup(context: Long)
//
//    companion object {
//        init {
//            System.loadLibrary("llamacpp") // Your native library name
//        }
//    }
//}
//
//// Compose UI for AI Management
//@Composable
//fun AIManagementScreen(
//    aiServiceManager: AIServiceManager
//) {
//    val serviceStatuses by aiServiceManager.serviceStatuses.collectAsState()
//    val availableModels by remember { mutableStateOf(emptyList<LocalModelInfo>()) }
//    val downloadingModels by remember { mutableStateOf(emptyMap<String, ModelDownloadProgress>()) }
//
//    LaunchedEffect(Unit) {
//        aiServiceManager.initializeServices()
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        Text(
//            text = "AI Services",
//            style = MaterialTheme.typography.headlineMedium,
//            modifier = Modifier.padding(bottom = 16.dp)
//        )
//
//        LazyColumn {
//            // Active Services Section
//            item {
//                ServiceStatusSection(
//                    serviceStatuses = serviceStatuses,
//                    onRemoveService = { provider ->
//                        // Handle service removal
//                    },
//                    onRefreshService = { provider ->
//                        // Handle service refresh
//                    },
//                    onSelectService = { provider, modelId ->
//                        // Handle service selection
//                    }
//                )
//            }
//
//            // Add Local Models Section
//            item {
//                Spacer(modifier = Modifier.height(24.dp))
//                AddLocalModelsSection(
//                    availableModels = availableModels,
//                    downloadingModels = downloadingModels,
//                    onDownloadModel = { model ->
//                        // Handle model download
//                    }
//                )
//            }
//
//            // Add Online Services Section
//            item {
//                Spacer(modifier = Modifier.height(24.dp))
//                AddOnlineServicesSection(
//                    onAddService = { provider, config ->
//                        // Handle adding online service
//                    }
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun ServiceStatusSection(
//    serviceStatuses: List<AIServiceStatus>,
//    onRemoveService: (AIProvider) -> Unit,
//    onRefreshService: (AIProvider) -> Unit,
//    onSelectService: (AIProvider, String?) -> Unit
//) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp)
//        ) {
//            Text(
//                text = "Configured Services",
//                style = MaterialTheme.typography.titleMedium,
//                modifier = Modifier.padding(bottom = 8.dp)
//            )
//
//            if (serviceStatuses.isEmpty()) {
//                Text(
//                    text = "No AI services configured",
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            } else {
//                serviceStatuses.forEach { status ->
//                    ServiceStatusItem(
//                        status = status,
//                        onRemove = { onRemoveService(status.provider) },
//                        onRefresh = { onRefreshService(status.provider) },
//                        onSelect = { modelId -> onSelectService(status.provider, modelId) }
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun ServiceStatusItem(
//    status: AIServiceStatus,
//    onRemove: () -> Unit,
//    onRefresh: () -> Unit,
//    onSelect: (String?) -> Unit
//) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(
//            containerColor = if (status.isAvailable)
//                MaterialTheme.colorScheme.primaryContainer
//            else
//                MaterialTheme.colorScheme.errorContainer
//        )
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(12.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Column(modifier = Modifier.weight(1f)) {
//                Text(
//                    text = status.provider.displayName,
//                    style = MaterialTheme.typography.titleSmall
//                )
//                Text(
//                    text = if (status.isAvailable) "Available" else "Not Available",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//
//            Row {
//                IconButton(onClick = { onSelect(null) }) {
//                    Icon(Icons.Default.PlayArrow, contentDescription = "Select")
//                }
//                IconButton(onClick = onRefresh) {
//                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
//                }
//                IconButton(onClick = onRemove) {
//                    Icon(Icons.Default.Delete, contentDescription = "Remove")
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun AddLocalModelsSection(
//    availableModels: List<LocalModelInfo>,
//    downloadingModels: Map<String, ModelDownloadProgress>,
//    onDownloadModel: (LocalModelInfo) -> Unit
//) {
//    var showAvailableModels by remember { mutableStateOf(false) }
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp)
//        ) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = "Local Models",
//                    style = MaterialTheme.typography.titleMedium
//                )
//                TextButton(
//                    onClick = { showAvailableModels = !showAvailableModels }
//                ) {
//                    Text(if (showAvailableModels) "Hide" else "Add Model")
//                    Icon(
//                        if (showAvailableModels) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
//                        contentDescription = null
//                    )
//                }
//            }
//
//            if (showAvailableModels) {
//                Spacer(modifier = Modifier.height(8.dp))
//                availableModels.forEach { model ->
//                    LocalModelItem(
//                        model = model,
//                        downloadProgress = downloadingModels[model.id],
//                        onDownload = { onDownloadModel(model) }
//                    )
//                    Spacer(modifier = Modifier.height(4.dp))
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun LocalModelItem(
//    model: LocalModelInfo,
//    downloadProgress: ModelDownloadProgress?,
//    onDownload: () -> Unit
//) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surface
//        )
//    ) {
//        Column(
//            modifier = Modifier.padding(12.dp)
//        ) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Column(modifier = Modifier.weight(1f)) {
//                    Text(
//                        text = model.name,
//                        style = MaterialTheme.typography.titleSmall
//                    )
//                    Text(
//                        text = model.description,
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                    Text(
//                        text = "Size: ${formatFileSize(model.size)} • RAM: ${model.requiredRam}MB",
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//
//                if (downloadProgress != null) {
//                    if (downloadProgress.isComplete) {
//                        Icon(
//                            Icons.Default.CheckCircle,
//                            contentDescription = "Downloaded",
//                            tint = MaterialTheme.colorScheme.primary
//                        )
//                    } else {
//                        CircularProgressIndicator(
//                            progress = downloadProgress.progressPercent / 100f,
//                            modifier = Modifier.size(24.dp)
//                        )
//                    }
//                } else {
//                    Button(
//                        onClick = onDownload,
//                        modifier = Modifier.height(32.dp)
//                    ) {
//                        Text("Download")
//                    }
//                }
//            }
//
//            if (downloadProgress != null && !downloadProgress.isComplete) {
//                Spacer(modifier = Modifier.height(8.dp))
//                LinearProgressIndicator(
//                    progress = downloadProgress.progressPercent / 100f,
//                    modifier = Modifier.fillMaxWidth()
//                )
//                Text(
//                    text = "${downloadProgress.progressPercent.toInt()}% - ${formatFileSize(downloadProgress.bytesDownloaded)} / ${formatFileSize(downloadProgress.totalBytes)}",
//                    style = MaterialTheme.typography.bodySmall,
//                    modifier = Modifier.padding(top = 4.dp)
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun AddOnlineServicesSection(
//    onAddService: (AIProvider, AIConfig) -> Unit
//) {
//    var showAddService by remember { mutableStateOf(false) }
//    var selectedProvider by remember { mutableStateOf<AIProvider?>(null) }
//    var apiKey by remember { mutableStateOf("") }
//    var isValidating by remember { mutableStateOf(false) }
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp)
//        ) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = "Online Services",
//                    style = MaterialTheme.typography.titleMedium
//                )
//                TextButton(
//                    onClick = { showAddService = !showAddService }
//                ) {
//                    Text(if (showAddService) "Cancel" else "Add Service")
//                    Icon(
//                        if (showAddService) Icons.Default.Close else Icons.Default.Add,
//                        contentDescription = null
//                    )
//                }
//            }
//
//            if (showAddService) {
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Provider Selection
//                Text(
//                    text = "Select Provider",
//                    style = MaterialTheme.typography.labelMedium,
//                    modifier = Modifier.padding(bottom = 8.dp)
//                )
//
//                val onlineProviders = listOf(
//                    AIProvider.ChatGPT,
//                    AIProvider.Claude,
//                    AIProvider.Groq,
//                    AIProvider.Ollama
//                )
//
//                LazyRow(
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    items(onlineProviders) { provider ->
//                        FilterChip(
//                            onClick = { selectedProvider = provider },
//                            label = { Text(provider.displayName) },
//                            selected = selectedProvider == provider
//                        )
//                    }
//                }
//
//                selectedProvider?.let { provider ->
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    if (provider.requiresApiKey) {
//                        OutlinedTextField(
//                            value = apiKey,
//                            onValueChange = { apiKey = it },
//                            label = { Text("API Key") },
//                            modifier = Modifier.fillMaxWidth(),
//                            visualTransformation = PasswordVisualTransformation(),
//                            singleLine = true
//                        )
//
//                        Spacer(modifier = Modifier.height(8.dp))
//                    }
//
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.End
//                    ) {
//                        Button(
//                            onClick = {
//                                isValidating = true
//                                val config = when (provider) {
//                                    AIProvider.ChatGPT -> AIConfig.ChatGPTConfig(apiKey)
//                                    AIProvider.Claude -> AIConfig.ClaudeConfig(apiKey)
//                                    AIProvider.Groq -> AIConfig.GroqConfig(apiKey)
//                                    AIProvider.Ollama -> AIConfig.OllamaConfig()
//                                    else -> return@Button
//                                }
//                                onAddService(provider, config)
//                                // Reset form
//                                selectedProvider = null
//                                apiKey = ""
//                                showAddService = false
//                                isValidating = false
//                            },
//                            enabled = !isValidating && (apiKey.isNotBlank() || !provider.requiresApiKey)
//                        ) {
//                            if (isValidating) {
//                                CircularProgressIndicator(
//                                    modifier = Modifier.size(16.dp),
//                                    strokeWidth = 2.dp
//                                )
//                                Spacer(modifier = Modifier.width(8.dp))
//                            }
//                            Text("Add Service")
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//private fun formatFileSize(bytes: Long): String {
//    return when {
//        bytes >= 1_000_000_000 -> "${bytes / 1_000_000_000}GB"
//        bytes >= 1_000_000 -> "${bytes / 1_000_000}MB"
//        bytes >= 1_000 -> "${bytes / 1_000}KB"
//        else -> "${bytes}B"
//    }
//}