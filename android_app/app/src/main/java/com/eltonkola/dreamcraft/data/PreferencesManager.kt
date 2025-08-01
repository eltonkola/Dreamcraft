package com.eltonkola.dreamcraft.data

import android.content.Context
import androidx.core.content.edit

class PreferencesManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    fun saveTheme(theme: String) {
        sharedPreferences.edit { putString("selected_theme", theme)}
    }

    fun getTheme(): String {
        return sharedPreferences.getString("selected_theme", "System") ?: "System"
    }

}
