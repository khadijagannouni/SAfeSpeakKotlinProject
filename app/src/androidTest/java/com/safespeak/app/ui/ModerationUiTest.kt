package com.safespeak.app.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.safespeak.app.domain.model.ModerationState
import com.safespeak.app.domain.model.ToxicityCategory
import com.safespeak.app.ui.components.OverrideDialog
import com.safespeak.app.ui.components.WarningBanner
import com.safespeak.app.ui.theme.SafeSpeakTheme
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for the moderation UX:
 *   • Warning banner displays for Toxic state.
 *   • Override dialog accepts justification and confirms.
 *
 * Maps to report §7 "UI Testing — warning display + override flow".
 */
class ModerationUiTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun warningBanner_isDisplayedForToxicState() {
        composeRule.setContent {
            SafeSpeakTheme {
                WarningBanner(
                    visible = true,
                    score = 0.85f,
                    categories = listOf(
                        ToxicityCategory.PROFANITY,
                        ToxicityCategory.HATE_SPEECH
                    )
                )
            }
        }
        composeRule.onNodeWithText("This message may be harmful").assertIsDisplayed()
    }

    @Test
    fun overrideDialog_showsTitleAndConfirmButton() {
        var confirmedJustification: String? = null
        composeRule.setContent {
            SafeSpeakTheme {
                OverrideDialog(
                    onConfirm = { j -> confirmedJustification = j },
                    onDismiss = { }
                )
            }
        }
        composeRule.onNodeWithText("Send flagged message?").assertIsDisplayed()
        composeRule.onNodeWithText("Send anyway").assertHasClickAction()
    }

    @Test
    fun overrideDialog_acceptsJustificationAndConfirms() {
        var confirmedJustification: String? = null
        composeRule.setContent {
            SafeSpeakTheme {
                OverrideDialog(
                    onConfirm = { j -> confirmedJustification = j },
                    onDismiss = { }
                )
            }
        }
        composeRule.onNodeWithText("Justification (optional)").performTextInput("I meant it")
        composeRule.onNodeWithText("Send anyway").performClick()
        assert(confirmedJustification == "I meant it") {
            "Expected justification text to be captured"
        }
    }
}
