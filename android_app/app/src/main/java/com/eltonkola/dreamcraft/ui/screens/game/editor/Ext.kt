package com.eltonkola.dreamcraft.ui.screens.game.editor

import android.R.attr.path
import android.content.Context
import android.net.Uri
import com.eltonkola.dreamcraft.core.model.FileItem
import com.eltonkola.dreamcraft.core.model.FileType
import com.eltonkola.dreamcraft.core.model.getFileType
import java.io.File


// Add this function to scan files from a directory
fun scanFilesFromPath(context: Context, path: String): List<FileItem> {
    return try {

        val projectsDir = File(context.filesDir, "projects")


        val directory = File(projectsDir, path)
        if (!directory.exists() || !directory.isDirectory) {
            emptyList()
        } else {
            directory.listFiles()?.mapNotNull { file ->
                if (file.isFile && !file.name.startsWith(".project.meta")) {
                    val content = try {
                        when (getFileType(file.name)) {
                            FileType.TEXT, FileType.LUA -> file.readText()
                            else -> ""
                        }
                    } catch (e: Exception) {
                        "" // If file can't be read, use empty content
                    }

                    FileItem(
                        id = file.absolutePath,
                        name = file.name,
                        type = getFileType(file.name),
                        content = content,
                        uri = Uri.fromFile(file)
                    )
                } else null
            } ?: emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }
}


fun formatTime(seconds: Float): String {
    val minutes = (seconds / 60).toInt()
    val remainingSeconds = (seconds % 60).toInt()
    return "%d:%02d".format(minutes, remainingSeconds)
}
