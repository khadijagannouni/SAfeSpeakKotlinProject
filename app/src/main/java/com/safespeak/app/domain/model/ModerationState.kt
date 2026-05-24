package com.safespeak.app.domain.model


sealed class ModerationState {
    data object Idle : ModerationState()

    /** Debounced typing — model not yet invoked. */
    data object Analyzing : ModerationState()

    /** Score below threshold. */
    data class Safe(val score: Float) : ModerationState()

    /** Score at/above threshold. Send is blocked unless overridden. */
    data class Warning(
        val score: Float,
        val categories: List<ToxicityCategory>
    ) : ModerationState()

    /** Inference exceeded the 100ms budget. Send allowed with caveat. */
    data class TimedOut(val latencyMs: Long) : ModerationState()
}
