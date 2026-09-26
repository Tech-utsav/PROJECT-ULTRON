package com.techiutsav.ultron.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.techiutsav.ultron.automation.AutomationExecutor
import com.techiutsav.ultron.data.ChatMessage
import com.techiutsav.ultron.data.MemoryFact
import com.techiutsav.ultron.data.SettingsStore
import com.techiutsav.ultron.data.UltronDatabase
import com.techiutsav.ultron.network.ClaudeApiClient
import com.techiutsav.ultron.voice.VoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val ULTRON_PERSONA = """
You are Ultron, a personal AI assistant running on the user's own Android phone.
You are helpful, direct, and a little dry-witted — not a supervillain, just a
capable assistant who happens to be named Ultron.

You have three abilities beyond chatting:
1. MEMORY — facts you're told to remember are listed below under "What I remember about the user."
   Use them naturally when relevant; don't recite the list back.
2. VOICE — your replies may be read aloud, so keep them conversational and not overly long.
3. AUTOMATION — when the user asks you to *do* something on their phone (open an app, call
   someone, text someone, set an alarm/timer, search the web, open a link), end your reply
   with exactly one line in this exact format, with no other text on that line:
   ACTION: {"type":"open_app","query":"whatsapp"}
   Valid "type" values and their fields:
   - open_app: {"query": "<app name to search for>"}
   - web_search: {"query": "<search text>"}
   - open_url: {"url": "<https://...>"}
   - call: {"number": "<phone number>"}
   - sms: {"number": "<phone number>", "message": "<text>"}
   - set_alarm: {"hour": <0-23>, "minute": <0-59>, "label": "<label>"}
   - set_timer: {"seconds": <number>}
   Only include an ACTION line when the user actually asked you to perform that action.
   Never invent one for ordinary conversation.
"""

data class UiMessage(val role: String, val text: String)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = UltronDatabase.getInstance(application)
    private val settings = SettingsStore(application)
    private val api = ClaudeApiClient()
    private val voice = VoiceManager(application).apply { init() }

    private val _messages = MutableStateFlow<List<UiMessage>>(emptyList())
    val messages: StateFlow<List<UiMessage>> = _messages.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _lastActionStatus = MutableStateFlow<String?>(null)
    val lastActionStatus: StateFlow<String?> = _lastActionStatus.asStateFlow()

    init {
        viewModelScope.launch {
            val recent = db.chatDao().recent(50).reversed()
            _messages.value = recent.map { UiMessage(it.role, it.content) }
        }
    }

    fun sendUserMessage(text: String, speakReply: Boolean) {
        if (text.isBlank()) return
        val userMsg = UiMessage("user", text)
        _messages.value = _messages.value + userMsg
        _isThinking.value = true

        viewModelScope.launch {
            db.chatDao().insert(ChatMessage(role = "user", content = text))

            val apiKey = settings.apiKey.first()
            if (apiKey.isBlank()) {
                val warning = "I don't have an API key yet — add one in Settings so I can think."
                _messages.value = _messages.value + UiMessage("assistant", warning)
                _isThinking.value = false
                return@launch
            }

            val facts = db.memoryDao().allFacts().map { it.fact }
            val systemPrompt = buildString {
                append(ULTRON_PERSONA)
                append("\n\nWhat I remember about the user:\n")
                if (facts.isEmpty()) append("(nothing yet)") else facts.forEach { append("- $it\n") }
            }

            val history = _messages.value.map { it.role to it.text }

            val result = api.sendMessage(apiKey, systemPrompt, history)
            _isThinking.value = false

            result.onSuccess { rawReply ->
                val action = AutomationExecutor.extractAction(rawReply)
                val cleanReply = AutomationExecutor.stripActionTag(rawReply)

                _messages.value = _messages.value + UiMessage("assistant", cleanReply)
                db.chatDao().insert(ChatMessage(role = "assistant", content = cleanReply))

                if (speakReply) voice.speak(cleanReply)

                if (action != null) {
                    val status = AutomationExecutor.execute(getApplication(), action)
                    _lastActionStatus.value = status
                }

                maybeExtractMemory(text)
            }.onFailure { err ->
                val errorMsg = "Something went wrong reaching Claude: ${err.message}"
                _messages.value = _messages.value + UiMessage("assistant", errorMsg)
            }
        }
    }

    /** Very simple heuristic: "remember that ..." / "my name is ..." get stored as facts. */
    private fun maybeExtractMemory(userText: String) {
        val lower = userText.lowercase()
        val trigger = when {
            lower.startsWith("remember that ") -> userText.substring(15)
            lower.startsWith("remember ") -> userText.substring(9)
            lower.contains("my name is ") -> userText
            else -> null
        } ?: return

        viewModelScope.launch {
            db.memoryDao().insert(MemoryFact(fact = trigger.trim()))
        }
    }

    fun listen(onError: (String) -> Unit) {
        voice.listenOnce(
            onResult = { text -> sendUserMessage(text, speakReply = true) },
            onError = onError
        )
    }

    fun clearHistory() {
        viewModelScope.launch {
            db.chatDao().clearAll()
            _messages.value = emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voice.destroy()
    }
}
