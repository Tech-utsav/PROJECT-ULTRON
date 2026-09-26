package com.techiutsav.ultron.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ultron_settings")

class SettingsStore(private val context: Context) {

    companion object {
        private val API_KEY = stringPreferencesKey("anthropic_api_key")
        private val VOICE_REPLY_ENABLED = stringPreferencesKey("voice_reply_enabled")
    }

    val apiKey: Flow<String> = context.dataStore.data.map { it[API_KEY] ?: "" }

    val voiceReplyEnabled: Flow<Boolean> = context.dataStore.data.map {
        (it[VOICE_REPLY_ENABLED] ?: "true") == "true"
    }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { it[API_KEY] = key }
    }

    suspend fun setVoiceReplyEnabled(enabled: Boolean) {
        context.dataStore.edit { it[VOICE_REPLY_ENABLED] = enabled.toString() }
    }
}
