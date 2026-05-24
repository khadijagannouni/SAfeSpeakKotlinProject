package com.safespeak.app.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.safespeak.app.ai.ToxicityClassifier
import com.safespeak.app.data.local.AppDatabase
import com.safespeak.app.data.repository.MessageRepository
import com.safespeak.app.domain.engine.MessageAnalysisEngine
import com.safespeak.app.domain.middleware.MessageAnalysisMiddleware
import com.safespeak.app.domain.model.ToxicityResult
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end integration test of the moderation pipeline:
 *
 *   ToxicityClassifier → MessageAnalysisEngine → MessageAnalysisMiddleware → Repository → Room
 *
 * Verifies that an outgoing message flows through every layer and the
 * final persisted row matches the moderation verdict.
 *
 * Maps to report §7.3 "Integration Testing — moderation pipeline".
 */
@RunWith(AndroidJUnit4::class)
class ModerationPipelineIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var middleware: MessageAnalysisMiddleware

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = MessageRepository(db.messageDao())
        val engine = MessageAnalysisEngine(ToxicityClassifier(context = null))
        middleware = MessageAnalysisMiddleware(engine, repo)
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun cleanMessage_passesThroughAndIsStoredAsSafe() = runTest {
        val text = "hello how are you today"
        val result = middleware.preAnalyze(text)
        assertTrue("Expected Safe verdict, got $result", result is ToxicityResult.Safe)
        middleware.persistSent(text, result, overridden = false)

        db.messageDao().observeAll().test {
            val rows = awaitItem()
            assertEquals(1, rows.size)
            assertEquals(false, rows.first().wasFlagged)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toxicMessage_isFlaggedAndPersistedWithCategories() = runTest {
        val text = "you are stupid and i hate you"
        val result = middleware.preAnalyze(text)
        assertTrue("Expected Toxic verdict, got $result", result is ToxicityResult.Toxic)
        middleware.persistSent(text, result, overridden = true, justification = "test")

        db.messageDao().observeAll().test {
            val row = awaitItem().first()
            assertEquals(true, row.wasFlagged)
            assertEquals(true, row.overridden)
            assertEquals("test", row.justification)
            assertTrue("Categories must be populated", row.categories.isNotBlank())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
