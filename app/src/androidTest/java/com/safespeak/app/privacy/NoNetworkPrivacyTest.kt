package com.safespeak.app.privacy

import android.net.TrafficStats
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.safespeak.app.ai.ToxicityClassifier
import com.safespeak.app.domain.engine.MessageAnalysisEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Privacy validation tests.
 *
 * Verifies that the moderation pipeline does NOT make any network calls.
 * Two layers of defence:
 *   1. Static: confirm no Retrofit/OkHttp/HttpURLConnection classes appear
 *      in the moderation code path (asserted via classloader).
 *   2. Runtime: snapshot TrafficStats before/after a batch of analyses;
 *      assert zero bytes transmitted by our UID.
 *
 * Maps to report §7.5 "Privacy Validation — verify no network calls".
 */
@RunWith(AndroidJUnit4::class)
class NoNetworkPrivacyTest {

    @Test
    fun moderation_doesNotTransmitAnyBytes() = runBlocking {
        val engine = MessageAnalysisEngine(ToxicityClassifier(context = null))

        val txBefore = TrafficStats.getUidTxBytes(android.os.Process.myUid())
        val rxBefore = TrafficStats.getUidRxBytes(android.os.Process.myUid())

        // Run a substantial batch
        repeat(50) {
            engine.analyze("hello world this is a test message")
            engine.analyze("you are stupid and i hate you")
            engine.analyze("this is a totally normal message")
        }

        val txAfter = TrafficStats.getUidTxBytes(android.os.Process.myUid())
        val rxAfter = TrafficStats.getUidRxBytes(android.os.Process.myUid())

        val tx = txAfter - txBefore
        val rx = rxAfter - rxBefore
        println("Bytes transmitted during moderation: tx=$tx rx=$rx")
        assertEquals("Moderation must not transmit any bytes", 0L, tx)
        assertEquals("Moderation must not receive any bytes",  0L, rx)
    }

    @Test
    fun noNetworkLibrariesPresentInModerationPath() {
        // These class names should NOT be loadable inside our app process.
        // If any of them resolves, the moderation path is at risk of leaking data.
        val forbidden = listOf(
            "retrofit2.Retrofit",
            "okhttp3.OkHttpClient",
            "com.google.firebase.FirebaseApp"
        )
        val loader = this::class.java.classLoader!!
        for (name in forbidden) {
            val cls: Class<*>? = try {
                Class.forName(name, false, loader)
            } catch (_: ClassNotFoundException) {
                null
            }
            assertNull("Forbidden network class present: $name", cls)
        }
    }
}
