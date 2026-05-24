package com.safespeak.app.ai

import com.safespeak.app.domain.model.ToxicityCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ToxicityClassifier].
 *
 * Verifies that the heuristic scorer:
 *   • returns different scores for different messages,
 *   • respects word boundaries (no substring false positives),
 *   • detects categories (profanity, hate, threat, harassment, offensive),
 *   • applies SHOUT and REPEAT amplifiers,
 *   • returns 0 for blank / empty input.
 */
class ToxicityClassifierTest {

    private val classifier = ToxicityClassifier(context = null)

    @Test
    fun `blank input returns zero score`() = runTest {
        val (score, cats) = classifier.classify("")
        assertEquals(0f, score, 0.001f)
        assertTrue(cats.isEmpty())
    }

    @Test
    fun `clean greeting is safe`() = runTest {
        val (score, _) = classifier.classify("hello, how are you today?")
        assertTrue("Expected safe score, got $score", score < ToxicityClassifier.THRESHOLD)
    }

    @Test
    fun `different clean messages can produce different scores than toxic ones`() = runTest {
        val (clean, _) = classifier.classify("the weather is nice today")
        val (toxic, _) = classifier.classify("you are a stupid idiot")
        assertNotEquals(
            "Clean and toxic messages must score differently",
            clean, toxic
        )
        assertTrue("Toxic ($toxic) should exceed clean ($clean)", toxic > clean)
    }

    @Test
    fun `profanity is flagged`() = runTest {
        val (score, cats) = classifier.classify("you are stupid")
        assertTrue("Score $score should exceed threshold", score >= ToxicityClassifier.THRESHOLD)
        assertTrue(
            "Expected PROFANITY in $cats",
            ToxicityCategory.PROFANITY in cats
        )
    }

    @Test
    fun `threat phrase is flagged as threat`() = runTest {
        val (score, cats) = classifier.classify("i'll hurt you")
        assertTrue("Threat must exceed threshold; got $score", score >= ToxicityClassifier.THRESHOLD)
        assertTrue("Expected THREAT in $cats", ToxicityCategory.THREAT in cats)
    }

    @Test
    fun `hate phrase is flagged as hate speech`() = runTest {
        val (score, cats) = classifier.classify("i hate you so much")
        assertTrue("Score=$score should exceed threshold", score >= ToxicityClassifier.THRESHOLD)
        assertTrue("Expected HATE_SPEECH in $cats", ToxicityCategory.HATE_SPEECH in cats)
    }

    @Test
    fun `harassment phrase is detected`() = runTest {
        val (score, cats) = classifier.classify("shut up nobody likes you")
        assertTrue("Score=$score should exceed threshold", score >= ToxicityClassifier.THRESHOLD)
        assertTrue("Expected HARASSMENT in $cats", ToxicityCategory.HARASSMENT in cats)
    }

    @Test
    fun `multiple categories detected together`() = runTest {
        val (_, cats) = classifier.classify("i hate you you stupid loser i'll hurt you")
        // At least two distinct categories should fire
        assertTrue("Expected ≥2 categories, got $cats", cats.size >= 2)
    }

    @Test
    fun `shouting amplifies the score`() = runTest {
        val (mild, _) = classifier.classify("you are annoying")
        val (loud, _) = classifier.classify("YOU ARE ANNOYING")
        assertTrue("LOUD ($loud) should exceed mild ($mild)", loud > mild)
    }

    @Test
    fun `repeated characters amplify the score`() = runTest {
        val (normal, _) = classifier.classify("stop")
        val (yelled, _) = classifier.classify("stoooop")
        assertTrue("yelled ($yelled) should exceed normal ($normal)", yelled > normal)
    }

    @Test
    fun `word boundary prevents substring false positives`() = runTest {
        // "ass" is not in our lexicon, but "stupid" is — let's pick a real overlap:
        // "class" must NOT trigger anything since "lass" / "ass" are not lexicon words.
        val (score, cats) = classifier.classify("the class meeting was great")
        assertTrue("Got unexpected hits: $cats with score $score", cats.isEmpty())
        assertTrue("Score should stay low for benign sentence", score < ToxicityClassifier.THRESHOLD)
    }

    @Test
    fun `score is bounded between 0 and 1`() = runTest {
        val text = "you stupid idiot moron loser pathetic worthless trash garbage"
        val (score, _) = classifier.classify(text)
        assertTrue("Score $score must be in 0..1", score in 0f..1f)
    }

    @Test
    fun `more hits produce monotonically higher scores`() = runTest {
        val (one, _) = classifier.classify("you are stupid")
        val (two, _) = classifier.classify("you are stupid and dumb")
        val (three, _) = classifier.classify("you are stupid and dumb and worthless")
        assertTrue("scores must be monotone: $one <= $two <= $three",
            one <= two && two <= three)
    }
}
