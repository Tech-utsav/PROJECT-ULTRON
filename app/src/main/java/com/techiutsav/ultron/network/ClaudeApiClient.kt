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
class ClaudeApiClient {

    // Free-tier model. You can swap this for another Gemini model name later.
    private val model = "gemini-3.8-flash"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
suspend fun sendMessage(
        apiKey: String,
        systemPrompt: String,
        history: List<Pair<String, String>>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val contentsArray = JSONArray()
            for ((role, content) in history) {
                val geminiRole = if (role == "assistant") "model" else "user"
                contentsArray.put(
                    JSONObject().apply {
                        put("role", geminiRole)
                        put("parts", JSONArray().put(JSONObject().apply { put("text", content) }))
                    }
                )
            }
val body = JSONObject().apply {
                put("contents", contentsArray)
                put(
                    "systemInstruction",
                    JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().apply { put("text", systemPrompt) }))
                    }
                )
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

            val request = Request.Builder()
                .url(url)
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("content-type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()
client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Gemini API error ${response.code}: $responseBody")
                    )
                }
                val json = JSONObject(responseBody)
                val candidates = json.getJSONArray("candidates")
                val text = StringBuilder()
                if (candidates.length() > 0) {
                    val parts = candidates.getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                    for (i in 0 until parts.length()) {
                        text.append(parts.getJSONObject(i).optString("text"))
                    }
                }
                Result.success(text.toString())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}