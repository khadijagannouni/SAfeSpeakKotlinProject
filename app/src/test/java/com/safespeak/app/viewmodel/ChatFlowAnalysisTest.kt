package com.safespeak.app.viewmodel

import com.safespeak.app.ai.ToxicityClassifier
import com.safespeak.app.domain.engine.MessageAnalysisEngine
import com.safespeak.app.domain.model.ToxicityCategory
import com.safespeak.app.domain.model.ToxicityResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lightweight ViewModel-shape tests for the analysis engine called from
 * the ChatViewModel's debounced flow.
 *
 * The full Android-aware ViewModel (AndroidViewModel) requires
 * instrumentation; here we cover the engine's behaviour with the real
 * heuristic classifier as it will be invoked by the ViewModel.
 *
 * Maps to report §7.2 "Unit Testing — analysis engine".
 */
class ChatFlowAnalysisTest {

    @Test
    fun `real classifier produces Safe for benign text`() = runTest {
        val engine = MessageAnalysisEngine(ToxicityClassifier(context = null))
        val result = engine.analyze("hello, how are you doing today?")
        assertTrue("Expected Safe, got $result", result is ToxicityResult.Safe)
    }

    @Test
    fun `real classifier produces Toxic with categories for hostile text`() = runTest {
        val engine = MessageAnalysisEngine(ToxicityClassifier(context = null))
        val result = engine.analyze("you stupid idiot i hate you")
        assertTrue("Expected Toxic, got $result", result is ToxicityResult.Toxic)
        val toxic = result as ToxicityResult.Toxic
        assertTrue("Should detect ≥1 category", toxic.categories.isNotEmpty())
        assertTrue(
            "Should detect PROFANITY or HATE_SPEECH; got ${toxic.categories}",
            ToxicityCategory.PROFANITY in toxic.categories ||
                ToxicityCategory.HATE_SPEECH in toxic.categories
        )
    }

    @Test
    fun `consecutive calls are independent`() = runTest {
        val engine = MessageAnalysisEngine(ToxicityClassifier(context = null))
        val r1 = engine.analyze("you are stupid")
        val r2 = engine.analyze("hello friend")
        assertTrue(r1 is ToxicityResult.Toxic)
        assertTrue(r2 is ToxicityResult.Safe)
    }
}
