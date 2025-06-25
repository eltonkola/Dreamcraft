package com.eltonkola.dreamcraft.data.local

import android.R.attr.type
import android.content.Context
import android.util.Log
import com.eltonkola.dreamcraft.core.ProjectConfig
import com.eltonkola.dreamcraft.core.loadProjectMetadata
import com.eltonkola.dreamcraft.core.projectTypes
import com.eltonkola.dreamcraft.core.saveProjectMetadata
import com.eltonkola.dreamcraft.data.FileManager
import com.eltonkola.dreamcraft.ui.screens.game.editor.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileManagerImpl(private val context: Context) : FileManager {

    override suspend fun saveFile(content: String, projectName: String, file: FileItem?): String = withContext(Dispatchers.IO) {
        val updatedPAth = updateProjectFile(context, projectName,  content, file)
        updatedPAth ?: ""
    }
}


suspend fun createProject(context: Context, projectName: String, type: ProjectConfig, file: FileItem?) {
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

        //save project type as metadata
        saveProjectMetadata(projectDir, type)

        val mainLuaFile = File(projectDir, file?.name ?: type.defaultName)
        mainLuaFile.createNewFile()
        Log.d("FileManager", "Project file: ${mainLuaFile.absolutePath} - ${mainLuaFile.exists()}")
    } catch (e: Exception) {
        // Handle error silently for now
        e.printStackTrace()
    }
}

suspend fun deleteProject(context: Context, projectName: String) {
    try {
        val projectsDir = File(context.filesDir, "projects")
        val projectDir = File(projectsDir, projectName)

        if (projectDir.exists()) {
            projectDir.deleteRecursively()
            Log.d("FileManager", "Deleted project: ${projectDir.absolutePath}")
        } else {
            Log.d("FileManager", "Project not found: ${projectDir.absolutePath}")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("FileManager", "Error deleting project $projectName: ${e.message}")
    }
}

suspend fun updateProjectFile(context: Context, projectName: String, content: String, file: FileItem?) : String? {
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

        val type = loadProjectMetadata(projectDir) ?: projectTypes.first()

        Log.d("FileManager", "Project dir: ${projectDir.absolutePath} - ${projectDir.exists()} -  IsDirectory: ${projectDir.isDirectory} ")
        val mainFile = File(projectDir, file?.name ?: type.defaultName)
        mainFile.writeText(content)
        Log.d("FileManager", "Project file: ${mainFile.absolutePath} - ${mainFile.exists()} -  IsDirectory: ${mainFile.isDirectory} ")

        mainFile.absolutePath
    } catch (e: Exception) {
        // Handle error silently for now
        e.printStackTrace()
        null
    }
}

data class DreamProject(
    val name: String,
    val nrFiles: Int,
    val createdAt: Date,
    val config: ProjectConfig
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
suspend fun loadProjects(context: Context): List<DreamProject> {
    return try {
        val projectsDir = File(context.filesDir, "projects")
        if (!projectsDir.exists()) {
            projectsDir.mkdirs()
        }
        projectsDir.listFiles()?.filter { it.isDirectory }
            ?.map { dir ->
                val nrFiles = dir.listFiles()?.size ?: 0
                val createdAt = Date(dir.lastModified())

                val config = loadProjectMetadata(dir) ?: projectTypes.first()

                DreamProject(name = dir.name, nrFiles = nrFiles, createdAt = createdAt, config = config)
            }?.sortedByDescending { it.createdAt }
            ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}
