package com.safespeak.app.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.safespeak.app.data.local.AppDatabase
import com.safespeak.app.data.repository.MessageRepository
import com.safespeak.app.domain.model.ToxicityCategory
import com.safespeak.app.domain.model.ToxicityResult
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test — Room database + Repository round-trip.
 *
 * Verifies:
 *   • Safe / Toxic / SafeWithTimeout results all persist with correct flags.
 *   • Insert then observe via reactive Flow emits the new row.
 *   • Override path is correctly persisted (overridden=true + justification).
 *
 * Maps to report §7.3 "Integration Testing — message persistence".
 */
@RunWith(AndroidJUnit4::class)
class RoomRepositoryIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: MessageRepository

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = MessageRepository(db.messageDao())
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun insertSafe_isStoredWithFlaggedFalse() = runTest {
        val id = repo.insert(
            content = "hello world",
            result = ToxicityResult.Safe(score = 0.10f, latencyMs = 42L),
            overridden = false,
            justification = null
        )
        assertTrue("id should be positive", id > 0)
        repo.observeAll().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            val row = list.first()
            assertEquals("hello world", row.content)
            assertEquals(false, row.wasFlagged)
            assertEquals(false, row.overridden)
            assertEquals(42L, row.latencyMs)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insertToxic_isStoredWithCategoriesAndFlagged() = runTest {
        repo.insert(
            content = "you are stupid",
            result = ToxicityResult.Toxic(
                score = 0.85f,
                latencyMs = 30L,
                categories = listOf(ToxicityCategory.PROFANITY)
            ),
            overridden = false,
            justification = null
        )
        repo.observeAll().test {
            val list = awaitItem()
            val row = list.first()
            assertEquals(true, row.wasFlagged)
            assertEquals(false, row.overridden)
            assertTrue("Categories should contain Profanity",
                row.categories.contains(ToxicityCategory.PROFANITY.label))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insertOverridden_persistsJustification() = runTest {
        repo.insert(
            content = "this is a toxic message",
            result = ToxicityResult.Toxic(
                score = 0.90f,
                latencyMs = 25L,
                categories = listOf(ToxicityCategory.HATE_SPEECH)
            ),
            overridden = true,
            justification = "I want to send it anyway"
        )
        repo.observeAll().test {
            val list = awaitItem()
            val row = list.first()
            assertEquals(true, row.overridden)
            assertEquals("I want to send it anyway", row.justification)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insertSafeWithTimeout_storesTimedOutFlag() = runTest {
        repo.insert(
            content = "hello",
            result = ToxicityResult.SafeWithTimeout(latencyMs = 100L),
            overridden = false,
            justification = null
        )
        repo.observeAll().test {
            val row = awaitItem().first()
            assertEquals(true, row.timedOut)
            assertEquals(false, row.wasFlagged)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun filterFlagged_returnsOnlyToxicRows() = runTest {
        repo.insert(
            "safe one", ToxicityResult.Safe(0.1f, 10L), false, null
        )
        repo.insert(
            "toxic one",
            ToxicityResult.Toxic(0.9f, 12L, listOf(ToxicityCategory.PROFANITY)),
            false, null
        )
        repo.observeFlagged().test {
            val flagged = awaitItem()
            assertEquals(1, flagged.size)
            assertEquals("toxic one", flagged.first().content)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
