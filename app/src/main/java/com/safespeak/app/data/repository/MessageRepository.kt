package com.safespeak.app.data.repository

import com.safespeak.app.data.local.MessageDao
import com.safespeak.app.data.local.MessageEntity
import com.safespeak.app.domain.model.ToxicityResult
import kotlinx.coroutines.flow.Flow

class MessageRepository(private val dao: MessageDao) {

    fun observeAll(): Flow<List<MessageEntity>> = dao.observeAll()
    fun observeSafe(): Flow<List<MessageEntity>> = dao.observeSafe()
    fun observeFlagged(): Flow<List<MessageEntity>> = dao.observeFlagged()
    fun observeOverridden(): Flow<List<MessageEntity>> = dao.observeOverridden()

    suspend fun insert(
        content: String,
        result: ToxicityResult,
        overridden: Boolean,
        justification: String?
    ): Long {
        val entity = when (result) {
            is ToxicityResult.Safe -> MessageEntity(
                content = content,
                timestamp = System.currentTimeMillis(),
                score = result.score,
                categories = "",
                wasFlagged = false,
                overridden = false,
                timedOut = false,
                justification = null,
                latencyMs = result.latencyMs
            )
            is ToxicityResult.Toxic -> MessageEntity(
                content = content,
                timestamp = System.currentTimeMillis(),
                score = result.score,
                categories = result.categories.joinToString(",") { it.label },
                wasFlagged = true,
                overridden = overridden,
                timedOut = false,
                justification = justification,
                latencyMs = result.latencyMs
            )
            is ToxicityResult.SafeWithTimeout -> MessageEntity(
                content = content,
                timestamp = System.currentTimeMillis(),
                score = result.score,
                categories = "",
                wasFlagged = false,
                overridden = false,
                timedOut = true,
                justification = null,
                latencyMs = result.latencyMs
            )
        }
        return dao.insert(entity)
    }

    suspend fun clear() = dao.clear()
}
