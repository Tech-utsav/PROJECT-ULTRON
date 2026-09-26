package com.techiutsav.ultron.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert
    suspend fun insert(message: ChatMessage)

    @Query("SELECT * FROM chat_messages ORDER BY id ASC")
    fun observeAll(): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages ORDER BY id DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ChatMessage>

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}

@Dao
interface MemoryDao {
    @Insert
    suspend fun insert(fact: MemoryFact)

    @Query("SELECT * FROM memory_facts ORDER BY id ASC")
    suspend fun allFacts(): List<MemoryFact>

    @Query("DELETE FROM memory_facts")
    suspend fun clearAll()
}
