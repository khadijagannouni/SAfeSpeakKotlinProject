package com.safespeak.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safespeak.app.data.local.MessageEntity
import com.safespeak.app.domain.model.ToxicityCategory
import com.safespeak.app.ui.theme.AccentSafe
import com.safespeak.app.ui.theme.AccentToxic
import com.safespeak.app.ui.theme.AccentWarn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WarningBanner(
    visible: Boolean,
    score: Float,
    categories: List<ToxicityCategory>
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AccentToxic.copy(alpha = 0.10f))
                .border(1.dp, AccentToxic.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                .padding(14.dp)
                .semantics {
                    contentDescription = "Warning: message may be harmful"
                    liveRegion = LiveRegionMode.Polite
                }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = "Warning icon",
                    tint = AccentToxic
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "This message may be harmful",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentToxic
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Our on-device moderator flagged it. Rephrase, or send with a reason.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(10.dp))
            ScoreBar(score = score)
            CategoryChips(categories)
        }
    }
}

@Composable
fun TimeoutBanner(visible: Boolean, latency: Long) {
    AnimatedVisibility(visible, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AccentWarn.copy(alpha = 0.10f))
                .border(1.dp, AccentWarn.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = AccentWarn
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Analysis timed out at ${latency}ms — sending with caveat.",
                style = MaterialTheme.typography.bodyMedium,
                color = AccentWarn
            )
        }
    }
}

@Composable
fun OverrideDialog(
    onConfirm: (justification: String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send flagged message?") },
        text = {
            Column {
                Text(
                    "This action is logged locally for audit. Add an optional justification.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Justification (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.ifBlank { "" }) }) {
                Text("Send anyway", color = AccentToxic, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

/** History list item — message bubble + dot + score + categories. */
@Composable
fun HistoryRow(entity: MessageEntity) {
    val dotColor = when {
        entity.overridden -> AccentToxic
        entity.wasFlagged -> AccentToxic
        entity.timedOut   -> AccentWarn
        else              -> AccentSafe
    }
    val tagText = when {
        entity.overridden -> "OVERRIDDEN"
        entity.wasFlagged -> "FLAGGED"
        entity.timedOut   -> "TIMEOUT"
        else              -> "SAFE"
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            StatusDot(color = dotColor)
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .width(2.dp)
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tagText,
                    color = dotColor,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = formatTime(entity.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = entity.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "score · %.2f".format(entity.score),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    text = "${entity.latencyMs}ms",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (entity.categories.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    entity.categories.split(",").take(4).forEach { label ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(AccentToxic.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentToxic
                            )
                        }
                    }
                }
            }
            if (!entity.justification.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "“${entity.justification}”",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}

private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
private fun formatTime(ts: Long) = timeFmt.format(Date(ts))

/**
 * A right-aligned outgoing chat bubble shown inline in the Compose screen
 * feed. Footer line shows the moderation outcome (safe / overridden / timeout)
 * so the sender sees the verdict immediately without having to swipe to History.
 */
@Composable
fun ChatBubble(entity: MessageEntity) {
    val isFlagged = entity.wasFlagged
    val isOverride = entity.overridden
    val isTimeout = entity.timedOut

    val bubbleBg: Color = when {
        isOverride -> AccentToxic.copy(alpha = 0.10f)
        isFlagged  -> AccentToxic.copy(alpha = 0.10f)
        isTimeout  -> AccentWarn.copy(alpha = 0.10f)
        else       -> MaterialTheme.colorScheme.surfaceVariant
    }
    val borderColor: Color = when {
        isOverride || isFlagged -> AccentToxic.copy(alpha = 0.4f)
        isTimeout               -> AccentWarn.copy(alpha = 0.4f)
        else                    -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    }
    val tagLabel = when {
        isOverride -> "Sent with override · ${"%.2f".format(entity.score)}"
        isFlagged  -> "Flagged · ${"%.2f".format(entity.score)}"
        isTimeout  -> "Sent (timeout) · ${entity.latencyMs}ms"
        else       -> "Safe · ${"%.2f".format(entity.score)}"
    }
    val tagColor = when {
        isOverride || isFlagged -> AccentToxic
        isTimeout               -> AccentWarn
        else                    -> AccentSafe
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Box(
                Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 4.dp
                        )
                    )
                    .background(bubbleBg)
                    .border(
                        1.dp,
                        borderColor,
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 4.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = entity.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(color = tagColor, size = 6.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = tagLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = tagColor
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatTime(entity.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}
