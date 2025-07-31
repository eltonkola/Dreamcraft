package com.eltonkola.dreamcraft.remote.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eltonkola.dreamcraft.core.data.RemoteFileDto
import com.eltonkola.dreamcraft.core.data.RemoteTaskFileSource
import com.eltonkola.dreamcraft.remote.data.ActiveAiConfig
import com.eltonkola.dreamcraft.remote.data.AiPreferencesRepository
import com.eltonkola.dreamcraft.remote.data.CloudServiceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

sealed interface DownloadState {
    object Idle : DownloadState
    data class Downloading(val progress: Int) : DownloadState
    data class Finished(val file: File) : DownloadState
    data class Error(val message: String) : DownloadState
}

@HiltViewModel
class LocalModelManagerViewModel @Inject constructor(
    private val context: Application,
    private val remoteSource: RemoteTaskFileSource,
    private val aiPreferencesRepository: AiPreferencesRepository
) : ViewModel() {

    // --- State for local model management ---
    private val _remoteFiles = MutableStateFlow<List<RemoteFileDto>>(emptyList())
    val remoteFiles: StateFlow<List<RemoteFileDto>> = _remoteFiles.asStateFlow()

    private val _localFiles = MutableStateFlow<List<File>>(emptyList())
    val localFiles: StateFlow<List<File>> = _localFiles.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    // --- NEW: State sourced from the AiRepository ---
    val activeAiConfig: StateFlow<ActiveAiConfig> = aiPreferencesRepository.activeAiConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActiveAiConfig.None)

    val savedApiKeys: StateFlow<Map<CloudServiceType, String>> = aiPreferencesRepository.savedApiKeys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    // ----------------------------------------------

    private val internalStorageDir = context.filesDir
    private val httpClient = OkHttpClient()

    init {
        viewModelScope.launch {
            loadRemoteFiles()
            refreshLocalFiles()
        }
    }

    // --- NEW: Methods to manage AI configuration ---
    fun selectAi(config: ActiveAiConfig) {
        viewModelScope.launch {
            aiPreferencesRepository.saveActiveAiConfig(config)
        }
    }

    fun saveApiKey(serviceType: CloudServiceType, apiKey: String) {
        viewModelScope.launch {
            aiPreferencesRepository.saveApiKey(serviceType, apiKey)
        }
    }

    fun deleteApiKey(serviceType: CloudServiceType) {
        viewModelScope.launch {
            if (activeAiConfig.value is ActiveAiConfig.Cloud &&
                (activeAiConfig.value as ActiveAiConfig.Cloud).serviceType == serviceType) {
                aiPreferencesRepository.saveActiveAiConfig(ActiveAiConfig.None)
            }
            aiPreferencesRepository.deleteApiKey(serviceType)
        }
    }

    private suspend fun loadRemoteFiles() {
        _remoteFiles.value = remoteSource.fetchRemoteTaskFiles()
    }

    fun refreshLocalFiles() {
        val files = internalStorageDir.listFiles()?.filter { it.name.endsWith(".task") } ?: emptyList()
        _localFiles.value = files
    }

    fun downloadRemoteFile(remoteFile: RemoteFileDto) {
        if (_downloadState.value is DownloadState.Downloading) return
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
                        val buffer = ByteArray(8 * 1024)
                        var bytes = input.read(buffer)
                        while (bytes >= 0) {
                            output.write(buffer, 0, bytes)
                            bytesCopied += bytes
                            if (totalBytes > 0) {
                                _downloadState.value = DownloadState.Downloading((bytesCopied * 100 / totalBytes).toInt())
                            }
                            bytes = input.read(buffer)
                        }
                    }
                }
                _downloadState.value = DownloadState.Finished(destinationFile)
                withContext(Dispatchers.Main) { refreshLocalFiles() }
            } catch (e: IOException) {
                Log.e("Download", "Error downloading file", e)
                if (destinationFile.exists()) destinationFile.delete()
                _downloadState.value = DownloadState.Error("Download failed: ${e.message}")
            }
        }
    }

    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }

    fun importFileFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val fileName = getFileName(context, uri)
            if (fileName?.endsWith(".task") == true) {
                val destFile = File(internalStorageDir, fileName)
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output -> input.copyTo(output) }
                    }
                    withContext(Dispatchers.Main) { refreshLocalFiles() }
                } catch (e: IOException) {
                    Log.e("Import", "Failed to import file from URI", e)
                }
            }
        }
    }

    fun deleteLocalFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            if (activeAiConfig.value is ActiveAiConfig.Local &&
                (activeAiConfig.value as ActiveAiConfig.Local).llmPath == file.absolutePath) {
                aiPreferencesRepository.saveActiveAiConfig(ActiveAiConfig.None)
            }
            if (file.exists() && file.delete()) {
                withContext(Dispatchers.Main) { refreshLocalFiles() }
            }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        // Your existing getFileName implementation
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }
    }
}


