package com.eltonkola.dreamcraft.remote.ui

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class LocalModelManagerViewModel @Inject constructor(
    private val context: Application,
    private val remoteSource: RemoteTaskFileSource
) : ViewModel() {

    private val _remoteFiles = MutableStateFlow<List<RemoteFileDto>>(emptyList())
    val remoteFiles: StateFlow<List<RemoteFileDto>> = _remoteFiles.asStateFlow()

    private val _localFiles = MutableStateFlow<List<File>>(emptyList())
    val localFiles: StateFlow<List<File>> = _localFiles.asStateFlow()

    private val internalStorageDir = context.filesDir

    init {
        viewModelScope.launch {
            loadRemoteFiles()
            refreshLocalFiles()
        }
    }

    private suspend fun loadRemoteFiles() {
        _remoteFiles.value = remoteSource.fetchRemoteTaskFiles()
    }

    fun refreshLocalFiles() {
        val files = internalStorageDir.listFiles()?.filter {
            it.extension == "task"
        } ?: emptyList()
        _localFiles.value = files
    }

    fun downloadRemoteFile(remoteFile: RemoteFileDto) {
        val request = DownloadManager.Request(Uri.parse(remoteFile.downloadUrl))
            .setTitle(remoteFile.name)
            .setDescription("Downloading ${remoteFile.name}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                remoteFile.name
            )

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }

    fun importFileFromUri(uri: Uri) {
        viewModelScope.launch {
            val fileName = getFileName(context, uri)
            if (fileName?.endsWith(".task") == true) {
                val destFile = File(internalStorageDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                refreshLocalFiles()
            }
        }
    }

    fun deleteLocalFile(file: File) {
        viewModelScope.launch {
            try {
                if (file.exists() && file.delete()) {
                    refreshLocalFiles()
                }
            } catch (e: Exception) {
                // Handle deletion error - you might want to show a toast or error message
                e.printStackTrace()
            }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
    }
}


interface RemoteTaskFileSource {
    suspend fun fetchRemoteTaskFiles(): List<RemoteFileDto>
}

class StaticRemoteSource : RemoteTaskFileSource {
    override suspend fun fetchRemoteTaskFiles(): List<RemoteFileDto> {
        return listOf(
            RemoteFileDto("base.task", "https://example.com/models/base.task"),
            RemoteFileDto("advanced.task", "https://example.com/models/advanced.task")
        )
    }
}




