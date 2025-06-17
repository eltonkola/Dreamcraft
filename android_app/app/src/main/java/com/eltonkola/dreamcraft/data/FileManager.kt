package com.eltonkola.dreamcraft.data

interface FileManager {
    suspend fun saveLuaFile(content: String): String
}