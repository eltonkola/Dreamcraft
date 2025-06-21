package com.eltonkola.dreamcraft.data.local

import android.content.Context
import android.os.Environment
import android.util.Log
import com.eltonkola.dreamcraft.data.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

data class GapeProject(
    val name: String,
    val nrFiles: Int,
    val createdAt: Date
) {
    fun timeAgo(): String {
        val now = System.currentTimeMillis()
        val diff = now - createdAt.time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hr ago"
            days < 7 -> "$days day${if (days > 1) "s" else ""} ago"
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(createdAt)
        }
    }
}

// File management functions
suspend fun loadProjects(context: Context): List<GapeProject> {
    return try {
        val projectsDir = File(context.filesDir, "projects")
        if (!projectsDir.exists()) {
            projectsDir.mkdirs()
        }
        projectsDir.listFiles()?.filter { it.isDirectory }
            ?.map { dir ->
                val nrFiles = dir.listFiles()?.size ?: 0
                val createdAt = Date(dir.lastModified())
                GapeProject(name = dir.name, nrFiles = nrFiles, createdAt = createdAt)
            }?.sortedByDescending { it.createdAt }
            ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}
