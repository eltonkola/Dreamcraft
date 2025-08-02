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
Create a complete HTML5 mobile game.

OUTPUT FORMAT:
Status: [Brief description of game being created]

\`\`\`html
[COMPLETE HTML code - do not truncate regardless of length]
\`\`\`

REQUIREMENTS:
- Single HTML file with embedded CSS/JavaScript
- Touch controls, fullscreen portrait layout
- Complete game logic including start/game/over states
- Generate ALL code needed for full functionality

GAME CONCEPT: ____

Provide status message then complete working HTML game.
""".trimIndent()
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
Generate a complete, responsive website.

- Single HTML file with embedded CSS (<style>).
- Clean, modern, professional design for mobile and desktop.
- Fullscreen layout with some top padding.
- No external libraries or CDNs.
- Semantic HTML5 only.
- No comments, no markdown, only raw HTML code.

Website idea: ____
""".trimIndent()
        ),
    )
}

