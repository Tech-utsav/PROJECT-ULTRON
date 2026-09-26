package com.techiutsav.ultron.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ChatMessage::class, MemoryFact::class], version = 1, exportSchema = false)
abstract class UltronDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile private var INSTANCE: UltronDatabase? = null

        fun getInstance(context: Context): UltronDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    UltronDatabase::class.java,
                    "ultron.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
