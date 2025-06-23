package com.eltonkola.dreamcraft.remote

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference

class LocalDemo(
    val modelPath: String,
    val context: Context
) {

   private val taskOptions = LlmInference.LlmInferenceOptions.builder()
        .setModelPath(modelPath)
        .setMaxTopK(64)

        .build()


    private val llmInference = LlmInference.createFromOptions(context, taskOptions)

    fun call(inputPrompt: String){
        val result = llmInference.generateResponse(inputPrompt)
        Log.i("", "result: $result")
    }


}