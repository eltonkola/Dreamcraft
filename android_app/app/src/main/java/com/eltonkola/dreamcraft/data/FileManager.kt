package com.eltonkola.dreamcraft.data

import com.eltonkola.dreamcraft.ui.screens.game.editor.FileItem

interface FileManager {
    suspend fun saveFile(content: String, projectName: String, file: FileItem?): String
}