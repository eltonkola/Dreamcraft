package com.eltonkola.dreamcraft.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import org.love2d.android.GameActivity
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.jvm.java

internal fun hasStoragePermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
}


fun Context.startGame(projectName: String) {
    val projectsDir = File(this.filesDir, "projects")
    val file = File(projectsDir, projectName)

    val gameFile = createLoveFileInFilesDir(this, file.absolutePath, "${projectName}.love")
    val intent = Intent(this, GameActivity::class.java)
    intent.data = Uri.fromFile(gameFile)
    startActivity(intent)
}

fun createLoveFileInFilesDir(context: Context, gameFolderName: String, outputFileName: String = "game.love"): File {

    Log.d("LoveGame", "Files in filesDir:")
    context.filesDir.listFiles()?.forEach {
        Log.d("LoveGame", it.name + " isDir=${it.isDirectory}")
    }

    val gameFolder = File(gameFolderName).let {
        if (it.isAbsolute) it else File(context.filesDir, gameFolderName)
    }

    Log.d("LoveGame", "Looking for folder: ${gameFolder.absolutePath}")
    Log.d("LoveGame", "Exists: ${gameFolder.exists()}, IsDirectory: ${gameFolder.isDirectory}")
    if (!gameFolder.isDirectory) {
        throw IllegalArgumentException("Game folder '$gameFolderName' does not exist or is not a directory")
    }

    val outputLoveFile = File(context.filesDir, outputFileName)
    if (outputLoveFile.exists()) {
        outputLoveFile.delete()
    }

    ZipOutputStream(BufferedOutputStream(FileOutputStream(outputLoveFile))).use { zipOut ->
        zipFolderContents(gameFolder, gameFolder, zipOut)
    }

    return outputLoveFile
}

private fun zipFolderContents(rootFolder: File, currentFolder: File, zipOut: ZipOutputStream) {
    val files = currentFolder.listFiles() ?: return
    for (file in files) {
        val entryName = file.relativeTo(rootFolder).path.replace(File.separatorChar, '/')
        if (file.isDirectory) {
            zipFolderContents(rootFolder, file, zipOut)
        } else {
            FileInputStream(file).use { input ->
                val zipEntry = ZipEntry(entryName)
                zipOut.putNextEntry(zipEntry)
                input.copyTo(zipOut)
                zipOut.closeEntry()
            }
        }
    }
}
