package com.eltonkola.dreamcraft.data

import com.eltonkola.dreamcraft.ui.screens.game.editor.FileItem

interface FileManager {
    suspend fun saveLuaFile(content: String, projectName: String, file: FileItem?): String
}