package com.eltonkola.dreamcraft.remote.data

sealed class AiIntegration(val name: String) {
    class GROQ(
        apiKey : String = "", //TODO - inject the key somehow
        val llmName: String = "Groq"
    ) : AiIntegration(llmName)
    class LOCAL(
        val llmPath: String,
        val llmName: String
    )  : AiIntegration(llmName)

    fun shortName() : String {
        return if(name.length > 4){
            name.substring(0, 4)
        }else{
            name
        }
    }
}

fun String.toAiResponse() : AiResponse {

    val regex = Regex("""<think>(.*?)</think>(.*)""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    val matchResult = regex.find(this.trim()) // Trim the input first

    return if (matchResult != null && matchResult.groupValues.size == 3) {
        val thoughtContent = matchResult.groupValues[1].trim()
        val codeContent = matchResult.groupValues[2].trim()
        AiResponse(
            thought = thoughtContent.ifEmpty { null },
            code = codeContent
        )
    } else {
        // If the pattern doesn't match (e.g., no <think> tags),
        // assume the entire response is code.
        AiResponse(thought = null, code = this.trim())
    }

}
