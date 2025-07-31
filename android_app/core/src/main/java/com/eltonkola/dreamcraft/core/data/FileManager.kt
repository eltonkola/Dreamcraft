package com.eltonkola.dreamcraft.core.data

import com.eltonkola.dreamcraft.core.model.FileItem

interface FileManager {
    suspend fun saveFile(content: String, projectName: String, file: FileItem?): String
}