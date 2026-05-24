package com.safespeak.app.domain

import com.safespeak.app.ai.ToxicityClassifier
import com.safespeak.app.domain.engine.MessageAnalysisEngine
import com.safespeak.app.domain.model.ToxicityCategory
import com.safespeak.app.domain.model.ToxicityResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [MessageAnalysisEngine] — the moderation rules layer.
 *
 * Covers:
 *   • threshold logic (Safe vs Toxic)
 *   • timeout handling (SafeWithTimeout fallback)
 *   • exception resilience (any throwable -> SafeWithTimeout, never crash)
 *   • latency stamping on every result
 *
 * Maps to report §7.2 "Unit Testing — moderation rules + timeout handling".
 */
class MessageAnalysisEngineTest {

    /** Fake classifier with deterministic score and optional delay. */
    private class FakeClassifier(
        private val score: Float,
        private val categories: List<ToxicityCategory> = emptyList(),
        private val delayMs: Long = 0L
    ) : ToxicityClassifier(context = null) {
        var invocations = 0
            private set
        override suspend fun classify(text: String): Pair<Float, List<ToxicityCategory>> {
            invocations++
            if (delayMs > 0) delay(delayMs)
            return score to categories
        }
    }

    // ── Moderation rules: threshold logic ─────────────────────────────────

    @Test
    fun `score below threshold returns Safe`() = runTest {
        val engine = MessageAnalysisEngine(FakeClassifier(0.20f), threshold = 0.55f)
        val result = engine.analyze("hello there")
        assertTrue("Expected Safe, got $result", result is ToxicityResult.Safe)
        assertEquals(0.20f, (result as ToxicityResult.Safe).score, 0.001f)
    }

    @Test
    fun `score exactly at threshold returns Toxic`() = runTest {
        val engine = MessageAnalysisEngine(
            FakeClassifier(0.55f, listOf(ToxicityCategory.PROFANITY)),
            threshold = 0.55f
        )
        val result = engine.analyze("borderline")
        assertTrue("Expected Toxic, got $result", result is ToxicityResult.Toxic)
        assertEquals(
            listOf(ToxicityCategory.PROFANITY),
            (result as ToxicityResult.Toxic).categories
        )
    }

    @Test
    fun `score well above threshold returns Toxic with all categories`() = runTest {
        val cats = listOf(ToxicityCategory.HATE_SPEECH, ToxicityCategory.HARASSMENT)
        val engine = MessageAnalysisEngine(FakeClassifier(0.92f, cats), threshold = 0.55f)
        val result = engine.analyze("...")
        assertTrue(result is ToxicityResult.Toxic)
        assertEquals(cats, (result as ToxicityResult.Toxic).categories)
    }

    // ── Timeout handling ──────────────────────────────────────────────────

    @Test
    fun `inference exceeding timeout returns SafeWithTimeout`() = runTest {
        val engine = MessageAnalysisEngine(
            FakeClassifier(0.10f, delayMs = 200L),
            timeoutMs = 50L
        )
        val result = engine.analyze("anything")
        assertTrue(
            "Expected SafeWithTimeout, got $result",
            result is ToxicityResult.SafeWithTimeout
        )
    }

    @Test
    fun `inference within budget does NOT time out`() = runTest {
        val engine = MessageAnalysisEngine(
            FakeClassifier(0.10f, delayMs = 10L),
            timeoutMs = 100L
        )
        val result = engine.analyze("hello")
        assertTrue(
            "Expected Safe, got $result",
            result is ToxicityResult.Safe
        )
    }

    // ── Resilience: classifier failure must not crash the UI ─────────────

    @Test
    fun `classifier throwing returns SafeWithTimeout instead of crashing`() = runTest {
        val broken = object : ToxicityClassifier(context = null) {
            override suspend fun classify(text: String): Pair<Float, List<ToxicityCategory>> {
                throw IllegalStateException("simulated TFLite failure")
            }
        }
        val engine = MessageAnalysisEngine(broken)
        val result = engine.analyze("hello")
        assertTrue(
            "Expected SafeWithTimeout fallback, got $result",
            result is ToxicityResult.SafeWithTimeout
        )
    }

    // ── Latency stamping: every result carries measured latency ──────────

    @Test
    fun `result includes measured latency`() = runTest {
        val engine = MessageAnalysisEngine(FakeClassifier(0.10f, delayMs = 5L))
        val result = engine.analyze("hello")
        assertTrue("Latency should be non-negative", result.latencyMs >= 0L)
    }

    // ── Each call invokes the classifier exactly once ────────────────────

    @Test
    fun `engine invokes classifier exactly once per message`() = runTest {
        val fake = FakeClassifier(0.10f)
        val engine = MessageAnalysisEngine(fake)
        engine.analyze("first")
        engine.analyze("second")
        assertEquals(2, fake.invocations)
    }
}
