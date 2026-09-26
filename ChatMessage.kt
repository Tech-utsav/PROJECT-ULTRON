package com.techiutsav.ultron.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single message in the conversation with Ultron.
 * role is either "user" or "assistant".
 */
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * A durable fact Ultron has learned about the user, e.g. "user's name is Utsav".
 * This is Ultron's long-term memory, separate from the scrolling chat log.
 */
@Entity(tableName = "memory_facts")
data class MemoryFact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fact: String,
    val timestamp: Long = System.currentTimeMillis()
)
