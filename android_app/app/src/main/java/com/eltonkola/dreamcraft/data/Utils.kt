package com.eltonkola.dreamcraft.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import org.love2d.android.GameActivity
import java.io.File
import kotlin.jvm.java

internal fun hasStoragePermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
}


fun Context.startGame(luaFileName: String) {
    val projectsDir = File(this.filesDir, "projects")
    val file = File(projectsDir, "$luaFileName/main.lua")

    val intent = Intent(this, GameActivity::class.java)
    intent.data = Uri.fromFile(file)

    startActivity(intent)
}


