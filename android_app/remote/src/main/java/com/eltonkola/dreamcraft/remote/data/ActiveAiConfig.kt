package com.eltonkola.dreamcraft.remote.data


import java.io.File

/**
 * An enum to represent the different types of cloud services we support.
 * This makes it easy to add new ones like OpenAI or Gemini in the future.
 */
enum class CloudServiceType {
    GROQ,
    OPENAI,
    GEMINI,
    CLAUDE,
    FAKE
}

/**
 * A flexible sealed class to represent the currently active AI configuration.
 * This will be saved to and loaded from the AiRepository.
 */
sealed class ActiveAiConfig {

    data object None : ActiveAiConfig()

    data class Cloud(
        val serviceType: CloudServiceType,
        val apiKey: String
    ) : ActiveAiConfig()

    data class Local(
        val llmPath: String,
        val llmName: String
    ) : ActiveAiConfig() {
        val file: File get() = File(llmPath)
    }

    fun name(): String{
        return when(this){
            is None -> "Mock"
            is Cloud -> this.serviceType.name
            is Local -> this.llmName
        }
    }

}
