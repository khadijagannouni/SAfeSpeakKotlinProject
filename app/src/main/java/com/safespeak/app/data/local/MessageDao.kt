package com.safespeak.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE wasFlagged = 0 ORDER BY timestamp DESC")
    fun observeSafe(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE wasFlagged = 1 ORDER BY timestamp DESC")
    fun observeFlagged(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE overridden = 1 ORDER BY timestamp DESC")
    fun observeOverridden(): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages")
    suspend fun clear()
}
