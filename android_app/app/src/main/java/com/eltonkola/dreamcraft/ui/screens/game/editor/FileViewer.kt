package com.eltonkola.dreamcraft.ui.screens.game.editor

import androidx.compose.runtime.Composable


@Composable
fun FileViewer(
    file: FileItem,
    onFileContentChanged: (String) -> Unit = {}
) {
    when (file.type) {
        FileType.LUA -> {
            LuaFileEditor(
                content = file.content,
                onContentChanged = onFileContentChanged
            )
        }
        FileType.TEXT -> {
            TextFileEditor(
                content = file.content,
                onContentChanged = onFileContentChanged
            )
        }
        FileType.IMAGE -> {
            ImageViewer(file = file)
        }
        FileType.AUDIO -> {
            AudioPlayer(file = file)
        }
    }
}

