/*
 * Copyright (C) 2025 Claude IME Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ai

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

class LocalAIClient(
    private val baseUrl: String = "http://localhost:11434",
    private val model: String = "qwen2.5:3b"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    data class GenerateRequest(
        val model: String,
        val prompt: String,
        val stream: Boolean = false,
        val options: Map<String, Any> = emptyMap()
    )

    data class GenerateResponse(
        @SerializedName("response") val response: String,
        @SerializedName("done") val done: Boolean = true
    )

    data class AICandidate(
        val text: String,
        val confidence: Float = 0.5f
    )

    suspend fun getContinuation(
        currentText: String,
        screenContext: String = "",
        maxTokens: Int = 30
    ): List<AICandidate> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(currentText, screenContext)
            val request = GenerateRequest(
                model = model,
                prompt = prompt,
                options = mapOf(
                    "temperature" to 0.4,
                    "num_predict" to maxTokens,
                    "stop" to listOf("\n\n", "User:", "Assistant:")
                )
            )

            val body = gson.toJson(request).toRequestBody("application/json".toMediaType())
            val httpRequest = Request.Builder()
                .url("$baseUrl/api/generate")
                .post(body)
                .build()

            client.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext emptyList()
                }
                val json = response.body?.string() ?: return@withContext emptyList()
                val result = gson.fromJson(json, GenerateResponse::class.java)
                parseResult(result.response, currentText)
            }
        } catch (e: Exception) {
            Timber.w("AI request failed: ${e.message}")
            emptyList()
        }
    }

    private fun buildPrompt(currentText: String, screenContext: String): String {
        return """你是一个智能输入法助手。根据上下文自然续写文本。
屏幕上下文: $screenContext
规则:
1. 续写要自然流畅
2. 不要重复已输入的内容
3. 只输出续写部分，不要加引号
当前文本: "$currentText"
续写:""".trimIndent()
    }

    private fun parseResult(response: String, original: String): List<AICandidate> {
        val clean = response.trim().replace("\"", "").lines().firstOrNull { it.isNotBlank() } ?: ""
        if (clean.isEmpty()) return emptyList()
        val continuation = if (clean.startsWith(original)) clean.removePrefix(original) else clean
        return listOf(AICandidate(continuation, 0.8f)).filter { it.text.isNotEmpty() }
    }

    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$baseUrl/api/tags").build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }
}
