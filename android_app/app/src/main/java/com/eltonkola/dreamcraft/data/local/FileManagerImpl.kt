package com.eltonkola.dreamcraft.data.local

import android.content.Context
import android.os.Environment
import android.util.Log
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
        Log.d("FileManager", "Projects dir: ${projectsDir.absolutePath}")

        val projectDir = File(projectsDir, projectName)
        if (!projectDir.exists()) {
            projectDir.mkdirs()
        }
        Log.d("FileManager", "Project dir: ${projectDir.absolutePath}")

        val mainLuaFile = File(projectDir, "main.lua")
        mainLuaFile.createNewFile()
        Log.d("FileManager", "Project file: ${mainLuaFile.absolutePath} - ${mainLuaFile.exists()}")
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
        Log.d("FileManager", "Projects dir: ${projectsDir.absolutePath} - ${projectsDir.exists()} -  IsDirectory: ${projectsDir.isDirectory} ")
        val projectDir = File(projectsDir, projectName)
        if (!projectDir.exists()) {
            projectDir.mkdirs()
        }
        Log.d("FileManager", "Project dir: ${projectDir.absolutePath} - ${projectDir.exists()} -  IsDirectory: ${projectDir.isDirectory} ")
        val mainLuaFile = File(projectDir, "main.lua")
        mainLuaFile.writeText(content)
        Log.d("FileManager", "Project file: ${mainLuaFile.absolutePath} - ${mainLuaFile.exists()} -  IsDirectory: ${mainLuaFile.isDirectory} ")

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
