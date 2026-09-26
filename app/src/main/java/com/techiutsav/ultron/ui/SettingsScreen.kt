package com.techiutsav.ultron.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.techiutsav.ultron.data.SettingsStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onClearHistory: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()

    var apiKey by remember { mutableStateOf("") }
    var voiceEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        settings.apiKey.collect { apiKey = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Anthropic API key", style = MaterialTheme.typography.titleMedium)
            Text(
                "Get one at console.anthropic.com. Ultron sends your messages directly " +
                    "to Claude using this key — it's stored only on your device.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("sk-ant-...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(onClick = { scope.launch { settings.setApiKey(apiKey.trim()) } }) {
                Text("Save key")
            }

            Divider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Speak replies out loud by default", modifier = Modifier.weight(1f))
                Switch(
                    checked = voiceEnabled,
                    onCheckedChange = {
                        voiceEnabled = it
                        scope.launch { settings.setVoiceReplyEnabled(it) }
                    }
                )
            }

            Divider()

            OutlinedButton(onClick = onClearHistory) {
                Text("Clear chat history")
            }
        }
    }
}
