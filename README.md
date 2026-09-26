# Ultron — your personal Android AI assistant

Full assistant app: chat, voice in/out, long-term memory, and on-device automation.
Built with Kotlin + Jetpack Compose, powered by the Claude API.

## What it does

- **Chat** — talk to Ultron in a normal chat UI.
- **Voice** — tap the mic to speak; Ultron transcribes it, thinks, and (optionally) speaks
  its reply back using the phone's text-to-speech engine.
- **Memory** — say "remember that I have a dentist appointment Friday" or "my name is Utsav"
  and Ultron stores it in a local database. Every future conversation includes what it
  remembers about you, so it stays personalized across app restarts.
- **Automation** — ask it to open an app ("open WhatsApp"), call or text someone, set an
  alarm or timer, search the web, or open a link, and it will actually do it using standard
  Android intents (the same mechanism any app uses to hand off to another app — nothing
  requires root or invasive permissions).

## Setup

1. Open this folder in **Android Studio** (Koala or newer recommended).
2. Let Gradle sync — it will download the Compose, Room, and OkHttp dependencies.
3. Get an API key from **console.anthropic.com** (Anthropic's developer console).
4. Run the app on a device or emulator, tap the ⚙️ Settings icon, paste your API key, and save.
5. Start chatting or tap the mic.

## Project layout

```
app/src/main/java/com/techiutsav/ultron/
  MainActivity.kt          entry point, navigation, permissions
  ui/                       Compose screens + ChatViewModel (orchestrates everything)
  data/                     Room database (chat history + memory facts) + settings storage
  network/                  Claude API client (OkHttp)
  voice/                    SpeechRecognizer (STT) + TextToSpeech (TTS) wrapper
  automation/               Parses Ultron's ACTION commands and runs them as Android intents
```

## How automation works

The system prompt (in `ChatViewModel.kt`) tells Claude that when the user asks it to *do*
something, it should end its reply with one line like:

```
ACTION: {"type":"open_app","query":"whatsapp"}
```

`AutomationExecutor.kt` parses that line, strips it out of the visible reply, and executes
the matching Android intent (open app / call / SMS / alarm / timer / web search / open URL).

## Notes and limits

- This is a real, runnable Android Studio project, but I couldn't compile or test-run it in
  this sandbox (no Android SDK / no network here) — you'll do the first build in Android
  Studio, so watch the Gradle output for anything that needs a small fix (dependency version
  bumps happen often).
- The model string in `ClaudeApiClient.kt` is set to `claude-sonnet-5` — change it if your
  API key has access to a different model.
- Automation is intentionally limited to safe, public Android intents (the same handoffs any
  app can use). It does not read or control other apps' screens — that would require an
  Accessibility Service with much broader (and riskier) permissions, which isn't included here.
- Your API key and chat history stay on-device; nothing is sent anywhere except directly to
  Anthropic's API when you send a message.
