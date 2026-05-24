package com.safespeak.app.domain.engine

import com.safespeak.app.ai.ToxicityClassifier
import com.safespeak.app.domain.model.ToxicityResult
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.system.measureTimeMillis

class MessageAnalysisEngine(
    private val classifier: ToxicityClassifier,
    private val timeoutMs: Long = TIMEOUT_MS,
    private val threshold: Float = ToxicityClassifier.THRESHOLD
) {

    suspend fun analyze(text: String): ToxicityResult {
        var result: ToxicityResult? = null
        val elapsed = measureTimeMillis {
            result = try {
                withTimeout(timeoutMs) {
                    val (score, categories) = classifier.classify(text)
                    if (score >= threshold) {
                        ToxicityResult.Toxic(score, 0L, categories)
                    } else {
                        ToxicityResult.Safe(score, 0L)
                    }
                }
            } catch (_: TimeoutCancellationException) {
                ToxicityResult.SafeWithTimeout(latencyMs = timeoutMs)
            } catch (t: Throwable) {
                // Any other failure (TFLite NPE, classifier error, etc.)
                // must NOT crash the UI. Fail-open with a timeout-like result.
                android.util.Log.e("AnalysisEngine", "Classifier failed", t)
                ToxicityResult.SafeWithTimeout(latencyMs = timeoutMs)
            }
        }
        // Stamp the actual measured latency onto the result.
        val safe = result ?: ToxicityResult.SafeWithTimeout(latencyMs = elapsed)
        return when (safe) {
            is ToxicityResult.Safe -> safe.copy(latencyMs = elapsed)
            is ToxicityResult.Toxic -> safe.copy(latencyMs = elapsed)
            is ToxicityResult.SafeWithTimeout -> safe.copy(latencyMs = elapsed)
        }
    }

    companion object {
        const val TIMEOUT_MS: Long = 100L
    }
}
