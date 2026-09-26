package com.techiutsav.ultron.automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import androidx.core.net.toUri
import org.json.JSONObject

/**
 * Ultron is instructed (via the system prompt) to emit a line like:
 *   ACTION: {"type":"open_app","query":"whatsapp"}
 * whenever the user asks it to *do* something rather than just talk.
 * This class parses that line out of a reply and executes it as a safe,
 * permission-scoped Android intent. Nothing here touches other apps'
 * internals or requires device-admin/accessibility privileges — it only
 * uses the same public intents any app can use to hand off to another app.
 */
object AutomationExecutor {

    private val ACTION_REGEX = Regex("""ACTION:\s*(\{.*})""")

    /** Strips the ACTION:{...} line out of a reply so it isn't shown to the user. */
    fun stripActionTag(reply: String): String {
        return reply.replace(ACTION_REGEX, "").trim()
    }

    fun extractAction(reply: String): JSONObject? {
        val match = ACTION_REGEX.find(reply) ?: return null
        return try {
            JSONObject(match.groupValues[1])
        } catch (e: Exception) {
            null
        }
    }

    /** Returns a short human-readable status string describing what it did (or why it couldn't). */
    fun execute(context: Context, action: JSONObject): String {
        return try {
            when (action.optString("type")) {
                "open_app" -> openApp(context, action.optString("query"))
                "web_search" -> {
                    val query = action.optString("query")
                    val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                        putExtra("query", query)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    "Searched the web for \"$query\"."
                }
                "open_url" -> {
                    val url = action.optString("url")
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    "Opened $url."
                }
                "call" -> {
                    val number = action.optString("number")
                    val intent = Intent(Intent.ACTION_DIAL, "tel:$number".toUri()).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    "Opened the dialer for $number."
                }
                "sms" -> {
                    val number = action.optString("number")
                    val message = action.optString("message")
                    val intent = Intent(Intent.ACTION_SENDTO, "smsto:$number".toUri()).apply {
                        putExtra("sms_body", message)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    "Drafted a text to $number."
                }
                "set_alarm" -> {
                    val hour = action.optInt("hour", 8)
                    val minute = action.optInt("minute", 0)
                    val label = action.optString("label", "Ultron alarm")
                    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(AlarmClock.EXTRA_HOUR, hour)
                        putExtra(AlarmClock.EXTRA_MINUTES, minute)
                        putExtra(AlarmClock.EXTRA_MESSAGE, label)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    "Set an alarm for %02d:%02d.".format(hour, minute)
                }
                "set_timer" -> {
                    val seconds = action.optInt("seconds", 60)
                    val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                        putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    "Started a $seconds second timer."
                }
                else -> "I don't know how to do that action yet."
            }
        } catch (e: Exception) {
            "Couldn't complete that action: ${e.message}"
        }
    }

    private fun openApp(context: Context, query: String): String {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(0)
        val match = apps.firstOrNull {
            pm.getApplicationLabel(it).toString().contains(query, ignoreCase = true)
        } ?: return "Couldn't find an installed app matching \"$query\"."

        val launchIntent = pm.getLaunchIntentForPackage(match.packageName)
            ?: return "Found \"$query\" but it can't be launched directly."
        context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "Opening ${pm.getApplicationLabel(match)}."
    }
}
