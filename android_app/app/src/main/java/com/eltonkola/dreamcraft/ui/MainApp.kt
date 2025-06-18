package com.eltonkola.dreamcraft.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.composables.Gamepad2
import com.composables.Globe
import com.composables.Settings
import com.eltonkola.dreamcraft.data.PreferencesManager
import com.eltonkola.dreamcraft.ui.screens.CreateScreen
import com.eltonkola.dreamcraft.ui.screens.ExploreScreen
import com.eltonkola.dreamcraft.ui.screens.SettingsScreen
import com.eltonkola.dreamcraft.ui.screens.game.GameScreen
import com.eltonkola.dreamcraft.ui.theme.DreamcraftTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    var selectedTheme by remember {
        mutableStateOf(preferencesManager.getTheme())
    }

    // When theme changes, save it
    LaunchedEffect(selectedTheme) {
        preferencesManager.saveTheme(selectedTheme)
    }



    DreamcraftTheme(
        selectedTheme = selectedTheme
    ) {


        Scaffold(
            bottomBar = {
                if (currentRoute != "chat/{projectName}") {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Gamepad2, contentDescription = "Create") },
                            label = { Text("Create") },
                            selected = currentRoute == "create",
                            onClick = { navController.navigate("create") }
                        )
                        NavigationBarItem(
                            icon = { Icon(Globe, contentDescription = "Explore") },
                            label = { Text("Explore") },
                            selected = currentRoute == "explore",
                            onClick = { navController.navigate("explore") }
                        )
                        NavigationBarItem(
                            icon = { Icon(Settings, contentDescription = "Settings") },
                            label = { Text("Settings") },
                            selected = currentRoute == "settings",
                            onClick = { navController.navigate("settings") }
                        )
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "create",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("create") { CreateScreen(navController) }
                composable("explore") { ExploreScreen() }
                composable("settings") {
                    SettingsScreen(
                        selectedTheme = selectedTheme,
                        updateSettings = {
                            selectedTheme = it
                        }
                    )
                }
                composable("chat/{projectName}") { backStackEntry ->
                    val projectName = backStackEntry.arguments?.getString("projectName") ?: ""
                    GameScreen(projectName, navController)
                }
            }
        }
    }

}
