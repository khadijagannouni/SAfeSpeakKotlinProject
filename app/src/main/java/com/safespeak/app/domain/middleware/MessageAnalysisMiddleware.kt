package com.safespeak.app.domain.middleware

import com.safespeak.app.data.repository.MessageRepository
import com.safespeak.app.domain.engine.MessageAnalysisEngine
import com.safespeak.app.domain.model.ToxicityResult

/**
 * Middleware that every outgoing message must traverse.
 *
 *  UI → MessageAnalysisMiddleware → AnalysisEngine → Decision → Repository
 *
 * Guarantees:
 *  - No message reaches the database without first being analyzed.
 *  - Override events are flagged as such and persisted alongside the score.
 *  - The middleware itself is stateless; concurrency safety comes from the
 *    coroutine scope of the caller.
 */
class MessageAnalysisMiddleware(
    private val engine: MessageAnalysisEngine,
    private val repository: MessageRepository
) {

    suspend fun preAnalyze(text: String): ToxicityResult = engine.analyze(text)

    suspend fun persistSent(
        text: String,
        result: ToxicityResult,
        overridden: Boolean = false,
        justification: String? = null
    ): Long {
        return repository.insert(
            content = text,
            result = result,
            overridden = overridden,
            justification = justification
        )
    }
}
