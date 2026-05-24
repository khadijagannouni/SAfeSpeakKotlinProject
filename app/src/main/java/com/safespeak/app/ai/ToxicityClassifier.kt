package com.safespeak.app.ai

import android.content.Context
import android.util.Log
import com.safespeak.app.domain.model.ToxicityCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp
import kotlin.math.min

open class ToxicityClassifier(private val context: Context? = null) {

    private val interpreter: Interpreter? by lazy { tryLoadModel() }

    private fun tryLoadModel(): Interpreter? {
        val ctx = context ?: return null
        return try {
            val afd = ctx.assets.openFd(MODEL_FILE)
            val input = FileInputStream(afd.fileDescriptor)
            val buffer: MappedByteBuffer = input.channel.map(
                FileChannel.MapMode.READ_ONLY,
                afd.startOffset,
                afd.declaredLength
            )
            Interpreter(buffer)
        } catch (e: Exception) {
            Log.i(TAG, "TFLite model not bundled; using heuristic fallback.")
            null
        }
    }

    open suspend fun classify(text: String): Pair<Float, List<ToxicityCategory>> =
        withContext(Dispatchers.Default) {
            if (text.isBlank()) return@withContext 0f to emptyList()
            interpreter?.let { /* real TFLite inference would go here */ }
            heuristicScore(text)
        }

    private fun heuristicScore(text: String): Pair<Float, List<ToxicityCategory>> {
        val lower = text.lowercase()
        // Word-boundary tokens to avoid substring false-positives.
        val tokens = TOKEN_REGEX.findAll(lower).map { it.value }.toSet()

        val hits = mutableMapOf<ToxicityCategory, Int>()
        var totalHits = 0

        // Single-token lookup
        for ((token, category) in WORD_INDEX) {
            if (token in tokens) {
                hits[category] = (hits[category] ?: 0) + 1
                totalHits++
            }
        }
        // Multi-word phrase lookup (case-insensitive contains)
        for ((phrase, category) in PHRASE_INDEX) {
            if (lower.contains(phrase)) {
                hits[category] = (hits[category] ?: 0) + 1
                totalHits++
            }
        }

        // Smooth saturating curve: 1 - e^(-0.55 * hits)
        // hits=1 -> 0.42, hits=2 -> 0.67, hits=3 -> 0.81, hits=5 -> 0.94
        val baseScore = if (totalHits == 0) 0f
                        else (1f - exp(-0.55f * totalHits)).coerceAtMost(0.95f)

        // SHOUT boost
        val letters = text.filter { it.isLetter() }
        val shoutBoost = if (letters.length > 8 &&
            letters.count { it.isUpperCase() } > letters.length * 0.6
        ) 0.15f else 0f

        // REPEAT boost
        val repeatBoost = if (REPEAT_REGEX.containsMatchIn(text)) 0.10f else 0f

        val score = min(1f, baseScore + shoutBoost + repeatBoost)
        val categories = if (score >= THRESHOLD) hits.keys.toList() else emptyList()

        Log.d(
            TAG,
            "score=$score hits=$totalHits shout=$shoutBoost repeat=$repeatBoost " +
                "cats=${categories.map { it.label }} text='${text.take(40)}'"
        )

        return score to categories
    }

    companion object {
        private const val TAG = "ToxicityClassifier"
        private const val MODEL_FILE = "toxicity.tflite"

        /** Threshold for flagging. Tuned for the heuristic scorer. */
        const val THRESHOLD = 0.55f

        private val TOKEN_REGEX = Regex("[a-z']+")
        private val REPEAT_REGEX = Regex("(.)\\1{3,}")

        // Kept compact and audit-friendly. Real deployment would swap in TFLite.

        private val PROFANITY = listOf(
            "damn", "damned", "crap", "stupid", "stupidly", "idiot", "idiots",
            "dumb", "dumbass", "moron", "moronic", "jerk", "freaking", "freakin",
            "screw", "screwed", "hell", "bloody", "bastard", "piss", "pissed",
            "shit", "shitty", "bs", "fuck", "fucking", "fucker", "fucked",
            "asshole", "douche", "douchebag", "wtf", "stfu"
        )

        private val HATE = listOf(
            "racist", "sexist", "bigot", "bigoted", "homophobe", "homophobic",
            "transphobe", "nazis", "nazi", "supremacist", "scum", "vermin",
            "subhuman", "savages", "infidel", "kike", "spic", "chink",
            "gook", "wetback", "tranny", "fag", "faggot", "dyke", "retard",
            "retarded"
        )

        private val HATE_PHRASES = listOf(
            "i hate you", "i hate them", "go back to", "you people",
            "your kind", "those people", "deserve to die", "should die",
            "should be deported", "shouldn't exist", "doesn't deserve"
        )

        private val THREAT = listOf(
            "kill", "killing", "murder", "stab", "shoot", "shooting",
            "punch", "strangle", "destroy", "annihilate", "execute",
            "hunt", "find", "track"
        )

        private val THREAT_PHRASES = listOf(
            "i'll hurt", "ill hurt", "kill you", "beat you", "watch out",
            "you're dead", "youre dead", "i'll find you", "ill find you",
            "you'll regret", "youll regret", "i'm coming for", "im coming for",
            "i will end", "i'll end", "ill end", "wipe you out",
            "break your", "smash your"
        )

        private val HARASSMENT = listOf(
            "shutup", "loser", "losers", "worthless", "useless", "pathetic",
            "creep", "creepy", "weirdo", "freak", "freaks", "nobody",
            "annoying", "irritating", "obnoxious"
        )

        private val HARASSMENT_PHRASES = listOf(
            "shut up", "nobody likes", "nobody cares", "no one likes",
            "no one cares", "you suck", "you're trash", "youre trash",
            "you're a loser", "youre a loser", "leave me alone",
            "get a life", "get lost"
        )

        private val OFFENSIVE = listOf(
            "ugly", "fat", "gross", "trash", "garbage", "filth", "filthy",
            "disgusting", "vile", "repulsive", "nasty", "horrible",
            "terrible", "awful", "lame", "weak"
        )

        // Reverse index: word -> category, built once at class load
        private val WORD_INDEX: Map<String, ToxicityCategory> = buildMap {
            PROFANITY  .forEach { put(it, ToxicityCategory.PROFANITY)  }
            HATE       .forEach { put(it, ToxicityCategory.HATE_SPEECH) }
            THREAT     .forEach { put(it, ToxicityCategory.THREAT)     }
            HARASSMENT .forEach { put(it, ToxicityCategory.HARASSMENT) }
            OFFENSIVE  .forEach { put(it, ToxicityCategory.OFFENSIVE)  }
        }

        // Multi-word phrase index
        private val PHRASE_INDEX: Map<String, ToxicityCategory> = buildMap {
            HATE_PHRASES       .forEach { put(it, ToxicityCategory.HATE_SPEECH) }
            THREAT_PHRASES     .forEach { put(it, ToxicityCategory.THREAT)     }
            HARASSMENT_PHRASES .forEach { put(it, ToxicityCategory.HARASSMENT) }
        }
    }
}
