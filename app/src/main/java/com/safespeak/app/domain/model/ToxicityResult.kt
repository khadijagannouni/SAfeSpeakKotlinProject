package com.safespeak.app.domain.model

/**
 * Sealed hierarchy representing every possible outcome of moderation.
 * - [Safe]            : score < threshold
 * - [Toxic]           : score >= threshold, includes detected categories
 * - [SafeWithTimeout] : inference exceeded the 100ms budget; fail-open with caveat
 *
 * Type-safe handling ensures the UI must handle every branch explicitly.
 */
sealed class ToxicityResult {
    abstract val score: Float
    abstract val latencyMs: Long

    data class Safe(
        override val score: Float,
        override val latencyMs: Long
    ) : ToxicityResult()

    data class Toxic(
        override val score: Float,
        override val latencyMs: Long,
        val categories: List<ToxicityCategory>
    ) : ToxicityResult()

    data class SafeWithTimeout(
        override val score: Float = 0f,
        override val latencyMs: Long
    ) : ToxicityResult()
}

enum class ToxicityCategory(val label: String) {
    HATE_SPEECH("Hate speech"),
    PROFANITY("Profanity"),
    THREAT("Threat"),
    HARASSMENT("Harassment"),
    OFFENSIVE("Offensive language")
}
