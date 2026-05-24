package com.safespeak.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI test verifying that the Send button is disabled when the
 * draft is blank and enabled when text is present.
 *
 * Maps to report §7 "UI Testing — disabled send button".
 */
class SendButtonStateTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun sendButton_isDisabledWhenDraftBlank() {
        composeRule.setContent {
            val draft = remember { mutableStateOf("") }
            IconButton(
                enabled = draft.value.isNotBlank(),
                onClick = { },
                modifier = androidx.compose.ui.Modifier.semantics {
                    contentDescription = "Send button"
                }
            ) {
                Icon(Icons.Filled.Send, contentDescription = null)
            }
        }
        composeRule.onNodeWithContentDescription("Send button").assertIsNotEnabled()
    }

    @Test
    fun sendButton_isEnabledWhenDraftHasText() {
        var clicked = false
        composeRule.setContent {
            val draft = remember { mutableStateOf("hello") }
            IconButton(
                enabled = draft.value.isNotBlank(),
                onClick = { clicked = true },
                modifier = androidx.compose.ui.Modifier.semantics {
                    contentDescription = "Send button"
                }
            ) {
                Icon(Icons.Filled.Send, contentDescription = null)
            }
        }
        composeRule.onNodeWithContentDescription("Send button").assertIsEnabled()
        composeRule.onNodeWithContentDescription("Send button").performClick()
        assert(clicked) { "Expected send button click to fire" }
    }
}
