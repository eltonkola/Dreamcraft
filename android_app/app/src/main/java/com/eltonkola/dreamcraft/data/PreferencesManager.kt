package com.eltonkola.dreamcraft.data

import android.content.Context
import com.eltonkola.dreamcraft.BuildConfig

class PreferencesManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    fun saveTheme(theme: String) {
        sharedPreferences.edit().putString("selected_theme", theme).apply()
    }

    fun getTheme(): String {
        return sharedPreferences.getString("selected_theme", "System") ?: "System"
    }

    fun saveApiKey(key: String) {
        sharedPreferences.edit().putString("api_key", key).apply()
    }

    fun getApiKey(): String {
        return sharedPreferences.getString("api_key", DEFAULT_API_KEY) ?: DEFAULT_API_KEY
    }

    companion object {
        const val DEFAULT_API_KEY = BuildConfig.GROQ_API_KEY
    }
}
