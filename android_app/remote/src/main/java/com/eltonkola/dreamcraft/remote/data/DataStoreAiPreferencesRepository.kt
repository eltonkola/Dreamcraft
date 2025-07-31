package com.eltonkola.dreamcraft.remote.data


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.IOException


class DataStoreAiPreferencesRepository(private val context: Context) : AiPreferencesRepository {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_settings")

    private object PreferencesKeys {
        val ACTIVE_AI_TYPE = stringPreferencesKey("active_ai_type")
        val ACTIVE_LOCAL_PATH = stringPreferencesKey("active_local_path")
        val ACTIVE_CLOUD_SERVICE_TYPE = stringPreferencesKey("active_cloud_service_type")
        val GROQ_API_KEY = stringPreferencesKey("groq_api_key")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val CLOUDE_API_KEY = stringPreferencesKey("cloude_api_key")


        val FAKE_KEY = stringPreferencesKey("fake_key")
    }

    override val activeAiConfig: Flow<ActiveAiConfig> = context.dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { preferences ->
            when (preferences[PreferencesKeys.ACTIVE_AI_TYPE]) {
                "LOCAL" -> {
                    val path = preferences[PreferencesKeys.ACTIVE_LOCAL_PATH] ?: ""
                    if (path.isNotEmpty() && File(path).exists()) ActiveAiConfig.Local(path, File(path).nameWithoutExtension) else ActiveAiConfig.None
                }
                "CLOUD" -> {
                    val serviceType = preferences[PreferencesKeys.ACTIVE_CLOUD_SERVICE_TYPE]?.let { CloudServiceType.valueOf(it) }
                    val apiKey = getApiKeyFromPreferences(serviceType, preferences)
                    if (serviceType != null && apiKey.isNotEmpty()) ActiveAiConfig.Cloud(serviceType, apiKey) else ActiveAiConfig.None
                }
                else -> ActiveAiConfig.None
            }
        }

    override val savedApiKeys: Flow<Map<CloudServiceType, String>> = context.dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { preferences ->
            CloudServiceType.values().mapNotNull { serviceType ->
                getApiKeyFromPreferences(serviceType, preferences).takeIf { it.isNotEmpty() }?.let { serviceType to it }
            }.toMap()
        }

    override suspend fun saveActiveAiConfig(config: ActiveAiConfig) {
        context.dataStore.edit { prefs ->
            when (config) {
                is ActiveAiConfig.None -> prefs.remove(PreferencesKeys.ACTIVE_AI_TYPE)
                is ActiveAiConfig.Local -> {
                    prefs[PreferencesKeys.ACTIVE_AI_TYPE] = "LOCAL"
                    prefs[PreferencesKeys.ACTIVE_LOCAL_PATH] = config.llmPath
                }
                is ActiveAiConfig.Cloud -> {
                    prefs[PreferencesKeys.ACTIVE_AI_TYPE] = "CLOUD"
                    prefs[PreferencesKeys.ACTIVE_CLOUD_SERVICE_TYPE] = config.serviceType.name
                }
            }
        }
    }

    override suspend fun saveApiKey(serviceType: CloudServiceType, apiKey: String) {
        context.dataStore.edit { it[getPreferenceKeyForService(serviceType)] = apiKey }
    }

    override suspend fun deleteApiKey(serviceType: CloudServiceType) {
        context.dataStore.edit { it.remove(getPreferenceKeyForService(serviceType)) }
    }

    private fun getPreferenceKeyForService(serviceType: CloudServiceType) = when (serviceType) {
        CloudServiceType.GROQ -> PreferencesKeys.GROQ_API_KEY
        CloudServiceType.OPENAI -> PreferencesKeys.OPENAI_API_KEY
        CloudServiceType.GEMINI -> PreferencesKeys.GEMINI_API_KEY
        CloudServiceType.CLAUDE -> PreferencesKeys.CLOUDE_API_KEY
        else -> PreferencesKeys.FAKE_KEY
    }

    private fun getApiKeyFromPreferences(type: CloudServiceType?, prefs: Preferences) =
        type?.let { prefs[getPreferenceKeyForService(it)] } ?: ""
}