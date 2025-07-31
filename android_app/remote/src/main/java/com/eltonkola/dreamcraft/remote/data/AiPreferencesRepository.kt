package com.eltonkola.dreamcraft.remote.data

import kotlinx.coroutines.flow.Flow

interface AiPreferencesRepository {
    // Manages the currently selected AI configuration
    val activeAiConfig: Flow<ActiveAiConfig>
    suspend fun saveActiveAiConfig(config: ActiveAiConfig)

    // Manages the API keys for all cloud services
    val savedApiKeys: Flow<Map<CloudServiceType, String>>
    suspend fun saveApiKey(serviceType: CloudServiceType, apiKey: String)
    suspend fun deleteApiKey(serviceType: CloudServiceType)
}