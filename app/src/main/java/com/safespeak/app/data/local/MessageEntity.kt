package com.safespeak.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val timestamp: Long,
    val score: Float,
    val categories: String,
    val wasFlagged: Boolean,
    val overridden: Boolean,
    val timedOut: Boolean,
    val justification: String?,
    val latencyMs: Long
)
