package com.eltonkola.dreamcraft.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DreamcraftTheme(
    selectedTheme: String = "System", // User's theme selection
    content: @Composable () -> Unit
) {
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // Determine if we should use dark theme based on user selection
    val isDarkTheme = when (selectedTheme) {
        "System" -> isSystemInDarkTheme()
        "Light" -> false
        "Dark" -> true
        "Dynamic" -> isSystemInDarkTheme() // Dynamic follows system for dark/light
        else -> isSystemInDarkTheme() // Fallback to system
    }

    // Your existing dark color scheme
    val darkColorScheme = darkColorScheme(primary = Color(0xFF66ffc7))

    // Select color scheme based on user preference
    val colorScheme = when (selectedTheme) {
        "Dynamic" -> {
            if (supportsDynamicColor) {
                if (isDarkTheme) {
                    dynamicDarkColorScheme(LocalContext.current)
                } else {
                    dynamicLightColorScheme(LocalContext.current)
                }
            } else {
                // Fallback to your existing logic
                if (isDarkTheme) darkColorScheme else expressiveLightColorScheme()
            }
        }
        "System" -> {
            when {
                supportsDynamicColor && isDarkTheme -> {
                    dynamicDarkColorScheme(LocalContext.current)
                }
                supportsDynamicColor && !isDarkTheme -> {
                    dynamicLightColorScheme(LocalContext.current)
                }
                isDarkTheme -> darkColorScheme
                else -> expressiveLightColorScheme()
            }
        }
        "Dark" -> darkColorScheme
        "Light" -> expressiveLightColorScheme()
        else -> {
            // Fallback to your original logic
            when {
                supportsDynamicColor && isDarkTheme -> {
                    dynamicDarkColorScheme(LocalContext.current)
                }
                supportsDynamicColor && !isDarkTheme -> {
                    dynamicLightColorScheme(LocalContext.current)
                }
                isDarkTheme -> darkColorScheme
                else -> expressiveLightColorScheme()
            }
        }
    }

    val shapes = Shapes(largeIncreased = RoundedCornerShape(36.0.dp))

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        shapes = shapes,
        content = content
    )
}

@Composable
fun getAvailableThemeOptions(): List<String> {
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return if (supportsDynamicColor) {
        listOf("System", "Light", "Dark", "Dynamic")
    } else {
        listOf("System", "Light", "Dark")
    }
}

