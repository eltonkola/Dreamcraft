package com.eltonkola.dreamcraft.ui.screens.game.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.wakaztahir.codeeditor.highlight.model.CodeLang
import com.wakaztahir.codeeditor.highlight.prettify.PrettifyParser
import com.wakaztahir.codeeditor.highlight.theme.CodeThemeType
import com.wakaztahir.codeeditor.highlight.utils.parseCodeAsAnnotatedString


@Composable
fun LuaFileEditor(
    content: String,
    onContentChanged: (String) -> Unit
) {


        val language = CodeLang.Lua

        val parser = remember { PrettifyParser() }
        val themeState by remember { mutableStateOf(CodeThemeType.Default) }
        val theme = remember(themeState) { themeState.theme() }

        fun parse(code: String): AnnotatedString {
            return parseCodeAsAnnotatedString(
                parser = parser,
                theme = theme,
                lang = language,
                code = code
            )
        }

        var textFieldValue by remember { mutableStateOf(TextFieldValue(parse(content.trimIndent()))) }

        val scrollState = rememberScrollState()

    Row(modifier = Modifier.fillMaxSize()) {

        // Line numbers column
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(horizontal = 4.dp)
                .fillMaxHeight()
        ) {
            val lines = textFieldValue.text.lines()
            lines.forEachIndexed { index, _ ->
                Text(
                    text = (index + 1).toString(),
                    color = MaterialTheme.colorScheme.onBackground.copy(.3f)
                )
            }
        }

        // Code editor
        BasicTextField(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState), // shared scroll
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it.copy(annotatedString = parse(it.text))
                onContentChanged(textFieldValue.text)
            }
        )
    }
}