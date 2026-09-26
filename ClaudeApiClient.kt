package com.techiutsav.ultron.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Minimal client for the Anthropic /v1/messages endpoint.
 *
 * You need your own Anthropic API key (console.anthropic.com) — Ultron never
 * ships with one baked in. Paste it into the Settings screen of the app.
 */
class ClaudeApiClient {

    // Change this to whichever model your API key has access to.
    private val model = "claude-sonnet-5"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * @param apiKey Anthropic API key
     * @param systemPrompt Ultron's persona + memory facts, sent as the system prompt
     * @param history list of (role, content) pairs — role is "user" or "assistant"
     * @return the assistant's reply text
     */
    suspend fun sendMessage(
        apiKey: String,
        systemPrompt: String,
        history: List<Pair<String, String>>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val messagesArray = JSONArray()
            for ((role, content) in history) {
                messagesArray.put(
                    JSONObject().apply {
                        put("role", role)
                        put("content", content)
                    }
                )
            }

            val body = JSONObject().apply {
                put("model", model)
                put("max_tokens", 1024)
                put("system", systemPrompt)
                put("messages", messagesArray)
            }

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Claude API error ${response.code}: $responseBody")
                    )
                }
                val json = JSONObject(responseBody)
                val content = json.getJSONArray("content")
                val text = StringBuilder()
                for (i in 0 until content.length()) {
                    val block = content.getJSONObject(i)
                    if (block.optString("type") == "text") {
                        text.append(block.optString("text"))
                    }
                }
                Result.success(text.toString())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
