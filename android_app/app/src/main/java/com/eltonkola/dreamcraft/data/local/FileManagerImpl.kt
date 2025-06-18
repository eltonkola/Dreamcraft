package com.eltonkola.dreamcraft.data.local

import android.content.Context
import android.os.Environment
import com.eltonkola.dreamcraft.data.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileManagerImpl(private val context: Context) : FileManager {

    override suspend fun saveLuaFile(content: String, projectName: String): String = withContext(Dispatchers.IO) {
        val updatedPAth = updateProjectFile(context, projectName, content)
        updatedPAth ?: ""
    }
}


suspend fun createProject(context: Context, projectName: String) {
    try {
        val projectsDir = File(context.filesDir, "projects")
        if (!projectsDir.exists()) {
            projectsDir.mkdirs()
        }

        val projectDir = File(projectsDir, projectName)
        if (!projectDir.exists()) {
            projectDir.mkdirs()

            // Create main.lua file
            val mainLuaFile = File(projectDir, "main.lua")
            mainLuaFile.createNewFile()
        }
    } catch (e: Exception) {
        // Handle error silently for now
        e.printStackTrace()
    }
}

suspend fun updateProjectFile(context: Context, projectName: String, content: String) : String? {
    return try {
        val projectsDir = File(context.filesDir, "projects")
        if (!projectsDir.exists()) {
            projectsDir.mkdirs()
        }

        val projectDir = File(projectsDir, projectName)
        if (!projectDir.exists()) {
            projectDir.mkdirs()
        }

        val mainLuaFile = File(projectDir, "main.lua")
        mainLuaFile.writeText(content)
        mainLuaFile.absolutePath

    } catch (e: Exception) {
        // Handle error silently for now
        e.printStackTrace()
        null
    }
}

// File management functions
suspend fun loadProjects(context: Context): List<String> {
    return try {
        val projectsDir = File(context.filesDir, "projects")
        if (!projectsDir.exists()) {
            projectsDir.mkdirs()
        }
        projectsDir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}
