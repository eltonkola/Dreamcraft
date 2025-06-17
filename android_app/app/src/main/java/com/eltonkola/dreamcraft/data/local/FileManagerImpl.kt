package com.eltonkola.dreamcraft.data.local

import android.content.Context
import android.os.Environment
import com.eltonkola.dreamcraft.data.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileManagerImpl(private val context: Context) : FileManager {

    override suspend fun saveLuaFile(content: String): String = withContext(Dispatchers.IO) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        val file = File(downloadsDir, "snake_game_${System.currentTimeMillis()}.lua")

        file.writeText(content)
        file.absolutePath
    }
}

