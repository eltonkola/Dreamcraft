package com.eltonkola.dreamcraft.core.model

import android.net.Uri
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.FileAudio2
import com.composables.FileCode
import com.composables.FileImage
import com.composables.FilePen
import java.io.File


// Data classes
data class FileItem(
    val id: String,
    val name: String,
    val type: FileType,
    val content: String = "",
    val uri: Uri? = null,
    val isSaved: Boolean = true
){
    fun saveFile(){
        uri?.path?.let {
            val file = File(it)
            file.writeText(content)
        }
    }
}

enum class FileType(val icon: ImageVector, val extensions: List<String>) {
    LUA(FileCode, listOf("lua")),
    TEXT(FilePen, listOf("txt", "md", "json", "xml", "html", "css", "js", "py", "java", "kt")),
    IMAGE(FileImage, listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")),
    AUDIO(FileAudio2, listOf("mp3", "wav", "ogg", "m4a", "flac"))
}

fun getFileType(fileName: String): FileType {
    val extension = fileName.substringAfterLast(".").lowercase()
    return FileType.values().find { it.extensions.contains(extension) } ?: FileType.TEXT
}
