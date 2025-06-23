package com.eltonkola.dreamcraft.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream


data class RemoteFileDto(
    val name: String,
    val downloadUrl: String
)


@Composable
fun LocalModelManagerScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: LocalModelManagerViewModel = hiltViewModel(),
) {

    val localFiles by viewModel.localFiles.collectAsState()
    val remoteFiles by viewModel.remoteFiles.collectAsState()

    val context = LocalContext.current
    val internalFilesDir = context.filesDir
    val scope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    val name = getFileName(context, uri)
                    val destFile = File(internalFilesDir, name ?: "imported_${System.currentTimeMillis()}.task")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    )

    Column(modifier = modifier.padding(16.dp)) {
        Text("📂 Local .task Files", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (localFiles.isEmpty()) {
            Text("No local files found.")
        } else {
            LazyColumn {
                items(localFiles) { file ->
                    Text(file.name, modifier = Modifier.padding(4.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        Text("🌐 Remote .task Files", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyColumn {
            items(remoteFiles) { remote ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(remote.name)
                    Button(onClick = { viewModel.downloadRemoteFile(remote) }) {
                        Text("Download")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            filePickerLauncher.launch("application/octet-stream")
        }) {
            Text("📥 Import .task file")
        }
    }
}

fun getFileName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        cursor.getString(nameIndex)
    }
}
