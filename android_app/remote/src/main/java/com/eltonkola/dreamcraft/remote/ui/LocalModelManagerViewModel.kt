package com.eltonkola.dreamcraft.remote.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

/**
 * A sealed interface to represent the detailed state of a download operation.
 * Your UI can observe this to show a progress bar, success message, or error alert.
 */
sealed interface DownloadState {
    object Idle : DownloadState
    data class Downloading(val progress: Int) : DownloadState // Progress as a percentage (0-100)
    data class Finished(val file: File) : DownloadState
    data class Error(val message: String) : DownloadState
}

@HiltViewModel
class LocalModelManagerViewModel @Inject constructor(
    private val context: Application,
    private val remoteSource: RemoteTaskFileSource
) : ViewModel() {

    private val _remoteFiles = MutableStateFlow<List<RemoteFileDto>>(emptyList())
    val remoteFiles: StateFlow<List<RemoteFileDto>> = _remoteFiles.asStateFlow()

    private val _localFiles = MutableStateFlow<List<File>>(emptyList())
    val localFiles: StateFlow<List<File>> = _localFiles.asStateFlow()

    // New StateFlow to track the detailed status of a download for the UI.
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val internalStorageDir = context.filesDir
    private val httpClient = OkHttpClient() // Use a single client instance

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
        // Corrected to use .name and check for the ".task" extension
        val files = internalStorageDir.listFiles()?.filter {
            it.name.endsWith(".task")
        } ?: emptyList()
        _localFiles.value = files
    }

    /**
     * This function replaces the old DownloadManager logic.
     * It manually downloads the file using OkHttp, which can handle complex URLs and allows for progress tracking.
     */
    fun downloadRemoteFile(remoteFile: RemoteFileDto) {
        // Prevent starting a new download if one is already active
        if (_downloadState.value is DownloadState.Downloading) {
            Log.w("Download", "Another download is already in progress.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _downloadState.value = DownloadState.Downloading(0)
            val destinationFile = File(internalStorageDir, remoteFile.name)

            try {
                val request = Request.Builder().url(remoteFile.downloadUrl).build()
                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) throw IOException("Download failed: ${response.message} (Code: ${response.code})")

                val body = response.body ?: throw IOException("Response body is null")
                val totalBytes = body.contentLength()
                var bytesCopied = 0L

                body.byteStream().use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        val buffer = ByteArray(8 * 1024) // 8KB buffer
                        var bytes = input.read(buffer)
                        while (bytes >= 0) {
                            output.write(buffer, 0, bytes)
                            bytesCopied += bytes
                            if (totalBytes > 0) {
                                val progress = (bytesCopied * 100 / totalBytes).toInt()
                                _downloadState.value = DownloadState.Downloading(progress)
                            }
                            bytes = input.read(buffer)
                        }
                    }
                }
                _downloadState.value = DownloadState.Finished(destinationFile)
                // Switch back to the main thread to refresh the file list
                withContext(Dispatchers.Main) {
                    refreshLocalFiles()
                }

            } catch (e: IOException) {
                Log.e("Download", "Error downloading file", e)
                // Clean up the partially downloaded file on error
                if (destinationFile.exists()) {
                    destinationFile.delete()
                }
                _downloadState.value = DownloadState.Error("Download failed: ${e.message}")
            }
        }
    }

    /**
     * Call this from your UI to reset the download state, for example,
     * after the user acknowledges a "Finished" or "Error" message.
     */
    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }

    // This function for importing local files remains unchanged as it works correctly.
    fun importFileFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val fileName = getFileName(context, uri)
            // Ensure the file has the correct extension before importing
            if (fileName?.endsWith(".task") == true) {
                val destFile = File(internalStorageDir, fileName)
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    // Refresh the file list on the main thread after successful import
                    withContext(Dispatchers.Main) {
                        refreshLocalFiles()
                    }
                } catch (e: IOException) {
                    Log.e("Import", "Failed to import file from URI", e)
                }
            }
        }
    }

    // This function for deleting local files remains unchanged.
    fun deleteLocalFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (file.exists() && file.delete()) {
                    withContext(Dispatchers.Main) {
                        refreshLocalFiles()
                    }
                }
            } catch (e: Exception) {
                Log.e("Delete", "Failed to delete file", e)
            }
        }
    }

    // This utility function remains unchanged.
    private fun getFileName(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }
    }
}


interface RemoteTaskFileSource {
    suspend fun fetchRemoteTaskFiles(): List<RemoteFileDto>
}

class StaticRemoteSource : RemoteTaskFileSource {
    override suspend fun fetchRemoteTaskFiles(): List<RemoteFileDto> {

        return listOf(

            RemoteFileDto("gemma-3n-E2B-it-int4.task", "https://huggingface.co/eltonkola/AndroidLiteRtModels/resolve/main/gemma-3n-E2B-it-int4.task"),
            RemoteFileDto("gemma-3n-E4B-it-int4.task", "https://huggingface.co/eltonkola/AndroidLiteRtModels/resolve/main/gemma-3n-E4B-it-int4.task"),

        )
    }
}

// Data class for remote file info - no changes needed
data class RemoteFileDto(val name: String, val downloadUrl: String)