package com.eltonkola.dreamcraft.core.data


interface RemoteTaskFileSource {
    suspend fun fetchRemoteTaskFiles(): List<RemoteFileDto>
}

class StaticRemoteSource : RemoteTaskFileSource {
    override suspend fun fetchRemoteTaskFiles(): List<RemoteFileDto> {

        return listOf(

            RemoteFileDto("gemma-3n-E2B-it-int4.task", "https://huggingface.co/eltonkola/AndroidLiteRtModels/resolve/main/gemma-3n-E2B-it-int4.task"),
            RemoteFileDto("gemma-3n-E4B-it-int4.task", "https://huggingface.co/eltonkola/AndroidLiteRtModels/resolve/main/gemma-3n-E4B-it-int4.task"),

            RemoteFileDto("Qwen2.5-1.5B-Instruct_seq128_q8_ekv1280.task", "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_seq128_q8_ekv1280.task"),
            RemoteFileDto("hammer2p1_05b_.task", "https://huggingface.co/litert-community/Hammer2.1-0.5b/resolve/main/hammer2p1_05b_.task"),

            //RemoteFileDto("Model.task", "https://huggingface.co/Model.task"),



        )
    }
}


// Data class for remote file info - no changes needed
data class RemoteFileDto(val name: String, val downloadUrl: String)
