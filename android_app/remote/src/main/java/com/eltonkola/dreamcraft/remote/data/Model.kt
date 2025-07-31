package com.eltonkola.dreamcraft.remote.data

import android.util.Log

fun String.toAiResponse(): AiResponse {

    Log.v("toAiResponse", this)

    val trimmedInput = this.trim()

    // 1. First try <think> pattern (original behavior)
    val thinkRegex = Regex("""<think>(.*?)</think>(.*)""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    thinkRegex.find(trimmedInput)?.let { match ->
        if (match.groupValues.size == 3) {
            return AiResponse(
                thought = match.groupValues[1].trim().takeIf { it.isNotEmpty() },
                code = match.groupValues[2].trim()
            )
        }
    }

    // 2. Try various code block patterns
    val codeBlockPatterns = listOf(
        Regex("```(?:[\\w]*\\n)?(.*?)```", RegexOption.DOT_MATCHES_ALL),  // ``` with optional language
        Regex("~~~(?:[\\w]*\\n)?(.*?)~~~", RegexOption.DOT_MATCHES_ALL),  // ~~~ with optional language
        Regex("(?s)^(.+?)^\\s*\\b(?:Here'?s|The code|Implementation):?\\s*$",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))  // Corrected: using setOf()
    )

    for (pattern in codeBlockPatterns) {
        pattern.find(trimmedInput)?.let { match ->
            val code = match.groupValues.last().trim()
            val thought = trimmedInput.replace(match.value, "")
                .trim()
                .takeIf { it.isNotEmpty() }
            return AiResponse(thought, code)
        }
    }

    // 3. Fallback - split on first code-looking segment
    val codeLikeSection = Regex("""(?s)(<[^>]+>.*</[^>]+>|\w+\s*\{.*?\})""")  // HTML tags or CSS blocks
    codeLikeSection.find(trimmedInput)?.let { match ->
        return AiResponse(
            thought = trimmedInput.replace(match.value, "").trim().takeIf { it.isNotEmpty() },
            code = match.value.trim()
        )
    }

    // 4. Ultimate fallback
    return AiResponse(thought = null, code = trimmedInput)
}