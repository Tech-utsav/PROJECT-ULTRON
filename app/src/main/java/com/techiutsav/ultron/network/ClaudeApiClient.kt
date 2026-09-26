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
 * Minimal client for Google's Gemini API (generativelanguage.googleapis.com).
 *
 * Uses Google AI Studio's free tier — get a free API key at aistudio.google.com,
 * no billing required. Paste it into Ultron's Settings screen.
 *
 * Class name and method signature match the earlier Claude-based client on
 * purpose, so nothing else in the app needed to change.
 */
class ClaudeApiClient {

    // Free-tier model. You can swap this for another Gemini model name later.
    private val model = "gemini-2.5-flash"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * @param apiKey Google AI Studio API key
     * @param systemPrompt Ultron's persona + memory facts
     * @param history list of (role, content) pairs — role is "user" or "assistant"
     * @return the assistant's reply text
     */
    suspend fun sendMessage(
        apiKey: String,
        systemPrompt: String,
        history: List<Pair<String, String>>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val contentsArr