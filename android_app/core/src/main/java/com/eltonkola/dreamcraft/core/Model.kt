package com.eltonkola.dreamcraft.core

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.Gamepad
import com.composables.Gamepad2
import com.composables.Globe
import java.io.File

fun saveProjectMetadata(folder: File, config: ProjectConfig) {
    val metaFile = File(folder, ".project.meta")
    metaFile.writeText(config.type.name)
}

fun loadProjectMetadata(folder: File): ProjectConfig? {
    val metaFile = File(folder, ".project.meta")
    if (!metaFile.exists()) return null
    val typeName = metaFile.readText().trim()
    val type = runCatching { ProjectType.valueOf(typeName) }.getOrNull() ?: return null
    return projectTypes.firstOrNull { it.type == type }
}


class ProjectConfig(
    val name: String,
    val defaultName: String,
    val promptTemplate : String,
    val extensions : List<String>,
    val type: ProjectType,
    val icon: ImageVector
)

enum class ProjectType{
    LOVE2D, WEB_GAME, WEB_PAGE
}

val projectTypes : List<ProjectConfig>  by lazy {
    listOf(
        ProjectConfig(
            name = "Love 2d game",
            defaultName = "main.lua",
            extensions = listOf("lua"),
            type = ProjectType.LOVE2D,
            icon = Gamepad2,
            promptTemplate = """
You are a Lua code generator.
Your task is to create a complete, playable Love2D (LÖVE) game in Lua.

Requirements:
- The game must be fully playable.
- It must use arrow key controls.
- It must be a single Lua source file with the complete code.
- Do not include any explanation, comments, or markdown.
- Output only plain Lua source code. Do not include ``` or any descriptive text.

Game idea: ____
""".trimIndent(),
        ),
        ProjectConfig(
            name = "Web game",
            defaultName = "index.html",
            extensions = listOf("html", "css", "js"),
            type = ProjectType.WEB_GAME,
            icon = Gamepad,
            promptTemplate = """
You are a JavaScript web game code generator.
Your task is to create a complete, playable web game using HTML5 Canvas, plain JavaScript, and modern, visually appealing CSS.
Requirements:
The game must be fully playable in a mobile browser.
It must use mobile swipe gestures for control (no keyboard or buttons).
It must be a single, standalone HTML file with embedded JavaScript and CSS.
The CSS must be visually appealing (modern, polished, and colorful).
Do not include any explanation, comments, or markdown.
Output only valid HTML code, starting with <!DOCTYPE html> and ending with </html>.
Game idea: ____
""".trimIndent(),
        ),
        ProjectConfig(
            name = "Web page",
            defaultName = "index.html",
            extensions = listOf("html", "css", "js"),
            type = ProjectType.WEB_PAGE,
            icon = Globe,
            promptTemplate = """
You are an HTML and CSS website generator.
Your task is to create a complete, responsive website using only HTML and CSS (no JavaScript unless required for basic interactivity).
Requirements:
The website must be fully responsive and look great on both desktop and mobile.
Use modern, beautiful, and clean CSS styling (e.g., layout, fonts, buttons, spacing).
Output must be a single HTML file with embedded CSS (<style> tag).
The design should feel professional and visually polished.
Do not use external libraries or CDNs (e.g., no Bootstrap, no Tailwind).
Use semantic HTML5 tags.
Do not include any explanation, markdown, or comments — output only the raw HTML code.
The website must be fully functional and visually complete.
Website idea: ____
""".trimIndent(),
        ),
    )
}

