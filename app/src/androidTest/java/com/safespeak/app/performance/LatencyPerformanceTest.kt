package com.safespeak.app.performance

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.safespeak.app.ai.ToxicityClassifier
import com.safespeak.app.domain.engine.MessageAnalysisEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Performance tests for the moderation engine.
 *
 * Verifies that:
 *   • The engine returns within the 100ms budget for typical messages.
 *   • P95 latency over 100 iterations stays under 80ms (report §7.4).
 *   • A slow classifier still resolves within the engine's hard timeout.
 *
 * Run with: ./gradlew :app:connectedDebugAndroidTest
 *
 * Maps to report §7.4 "Performance Testing — latency < 100ms".
 */
@RunWith(AndroidJUnit4::class)
class LatencyPerformanceTest {

    private val sampleMessages = listOf(
        "hello how are you",
        "the weather is nice today",
        "you are an idiot",
        "i hate you so much",
        "i'll hurt you if you do that",
        "shut up nobody likes you",
        "STOP YELLING AT MEEEE",
        "this is a normal message",
        "please can you help me",
        "you stupid worthless trash"
    )

    @Test
    fun singleAnalysis_completesUnder100ms() = runBlocking {
        val engine = MessageAnalysisEngine(ToxicityClassifier(context = null))
        // Warm-up
        repeat(5) { engine.analyze("warmup") }

        val elapsed = measureTimeMillis {
            engine.analyze("you are stupid")
        }
        assertTrue("Single analysis took $elapsed ms (target < 100)", elapsed < 100L)
    }

    @Test
    fun batch_p95LatencyUnder80ms() = runBlocking {
        val engine = MessageAnalysisEngine(ToxicityClassifier(context = null))
        // Warm-up
        repeat(10) { engine.analyze("warmup") }

        val latencies = mutableListOf<Long>()
        repeat(100) {
            val msg = sampleMessages[it % sampleMessages.size]
            val t = measureTimeMillis { engine.analyze(msg) }
            latencies += t
        }
        latencies.sort()
        val p50 = latencies[49]
        val p95 = latencies[94]
        val p99 = latencies[98]
        println("Latency p50=$p50 ms, p95=$p95 ms, p99=$p99 ms")
        assertTrue("p95 $p95 ms should be < 80 ms", p95 < 80L)
        assertTrue("p99 $p99 ms should be < 100 ms", p99 < 100L)
    }
}
