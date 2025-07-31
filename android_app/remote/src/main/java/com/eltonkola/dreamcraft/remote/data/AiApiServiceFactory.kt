package com.eltonkola.dreamcraft.remote.data


import android.content.Context
import com.eltonkola.dreamcraft.remote.data.services.ClaudeApiService
import com.eltonkola.dreamcraft.remote.data.services.FakeApiServiceImpl
import com.eltonkola.dreamcraft.remote.data.services.GeminiApiService
import com.eltonkola.dreamcraft.remote.data.services.GroqApiService
import com.eltonkola.dreamcraft.remote.data.services.LocalModelApiService
import com.eltonkola.dreamcraft.remote.data.services.OpenAiApiService
import okhttp3.OkHttpClient

class AiApiServiceFactory(
    private val context: Context,
    private val client: OkHttpClient
) {

    private var lastConfig: ActiveAiConfig? = null
    private var cachedService: AiApiService? = null

    fun create(config: ActiveAiConfig): AiApiService? {
        if (config == lastConfig && cachedService != null) {
            return cachedService
        }


        val newService =  when (config) {
            is ActiveAiConfig.Cloud -> {
                when (config.serviceType) {
                    CloudServiceType.GROQ -> GroqApiService(client, config.apiKey)
                    CloudServiceType.OPENAI -> OpenAiApiService(client, config.apiKey)
                    CloudServiceType.CLAUDE -> ClaudeApiService(client, config.apiKey)
                    CloudServiceType.GEMINI -> GeminiApiService(client, config.apiKey)
                    CloudServiceType.FAKE -> FakeApiServiceImpl()
                }
            }
            is ActiveAiConfig.Local -> {
                LocalModelApiService(context, config.llmPath)
            }
            is ActiveAiConfig.None -> {
                null
            }
        }

        lastConfig = config
        cachedService = newService
        return newService

    }
}
