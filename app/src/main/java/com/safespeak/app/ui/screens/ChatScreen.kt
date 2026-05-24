package com.safespeak.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safespeak.app.domain.model.ModerationState
import com.safespeak.app.ui.components.ChatBubble
import com.safespeak.app.ui.components.OverrideDialog
import com.safespeak.app.ui.components.ScoreBar
import com.safespeak.app.ui.components.StatPill
import com.safespeak.app.ui.components.StatusDot
import com.safespeak.app.ui.components.TimeoutBanner
import com.safespeak.app.ui.components.WarningBanner
import com.safespeak.app.ui.theme.AccentSafe
import com.safespeak.app.ui.theme.AccentToxic
import com.safespeak.app.ui.theme.AccentWarn
import com.safespeak.app.ui.theme.Lime

@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val state by viewModel.moderationState.collectAsStateWithLifecycle()
    val showOverride by viewModel.showOverrideDialog.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val lastError by viewModel.lastError.collectAsStateWithLifecycle()

    val isToxic = state is ModerationState.Warning
    val isTimedOut = state is ModerationState.TimedOut

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(lastError) {
        lastError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            Header(state)

            // Chat feed: newest at the bottom (reverseLayout = true keeps
            // the latest message visible above the composer).
            val listState = rememberLazyListState()
            LaunchedEffect(recent.size) {
                if (recent.isNotEmpty()) listState.animateScrollToItem(0)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = true,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = 8.dp,
                    bottom = 8.dp
                )
            ) {
                items(items = recent.take(50), key = { it.id }) { msg ->
                    ChatBubble(msg)
                }
                if (recent.isEmpty()) {
                    item { EmptyFeedHint() }
                }
            }

            // Banner area (warning / timeout)
            WarningBanner(
                visible = isToxic,
                score = (state as? ModerationState.Warning)?.score ?: 0f,
                categories = (state as? ModerationState.Warning)?.categories ?: emptyList()
            )
            TimeoutBanner(
                visible = isTimedOut,
                latency = (state as? ModerationState.TimedOut)?.latencyMs ?: 0L
            )

            ComposerCard(
                draft = draft,
                state = state,
                onTextChange = viewModel::onDraftChange,
                onSend = { viewModel.onSend() },
                onRephrase = viewModel::rephrase
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = AccentToxic,
                contentColor = Color.White
            )
        }
    }

    if (showOverride) {
        OverrideDialog(
            onConfirm = { just ->
                viewModel.onSend(forceOverride = true, justification = just)
            },
            onDismiss = viewModel::dismissOverrideDialog
        )
    }
}

@Composable
private fun EmptyFeedHint() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Start typing below.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Every message is moderated on-device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
private fun Header(state: ModerationState) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Lime)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "MODERATOR · LIVE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Compose",
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black
            )
        }
        StateBadge(state)
    }
}

@Composable
private fun StateBadge(state: ModerationState) {
    val (label, color) = when (state) {
        ModerationState.Idle          -> "READY"     to MaterialTheme.colorScheme.outline
        ModerationState.Analyzing     -> "ANALYZING" to AccentWarn
        is ModerationState.Safe       -> "SAFE"      to AccentSafe
        is ModerationState.Warning    -> "FLAGGED"   to AccentToxic
        is ModerationState.TimedOut   -> "TIMEOUT"   to AccentWarn
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(color = color, size = 7.dp)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun ComposerCard(
    draft: String,
    state: ModerationState,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onRephrase: () -> Unit
) {
    val isToxic = state is ModerationState.Warning
    val borderColor by animateColorAsState(
        targetValue = when (state) {
            is ModerationState.Warning -> AccentToxic
            is ModerationState.Safe    -> AccentSafe.copy(alpha = 0.55f)
            is ModerationState.TimedOut -> AccentWarn
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        },
        animationSpec = tween(220), label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isToxic) AccentToxic.copy(alpha = 0.06f)
                      else MaterialTheme.colorScheme.surface,
        animationSpec = tween(220), label = "bg"
    )

    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        tonalElevation = 0.dp
    ) {
        Column(Modifier.padding(14.dp)) {
            // Live score readout
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val scoreLabel = when (state) {
                    is ModerationState.Safe    -> "%.2f".format(state.score)
                    is ModerationState.Warning -> "%.2f".format(state.score)
                    is ModerationState.TimedOut -> "—"
                    ModerationState.Analyzing -> "…"
                    ModerationState.Idle      -> "0.00"
                }
                StatPill(label = "Score", value = scoreLabel)

                val latencyText = when (state) {
                    is ModerationState.Safe    -> "<100ms"
                    is ModerationState.Warning -> "<100ms"
                    is ModerationState.TimedOut -> "${state.latencyMs}ms"
                    else -> "—"
                }
                StatPill(label = "Latency", value = latencyText)
            }
            Spacer(Modifier.height(10.dp))
            ScoreBar(
                score = when (state) {
                    is ModerationState.Safe -> state.score
                    is ModerationState.Warning -> state.score
                    else -> 0f
                }
            )
            Spacer(Modifier.height(14.dp))

            // Text field. Placeholder is rendered ONLY when draft is empty
            // (no overlap with the actual input).
            Box(Modifier.fillMaxWidth()) {
                if (draft.isEmpty()) {
                    Text(
                        text = "Type a message…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
                    )
                }
                BasicTextField(
                    value = draft,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Message draft input" },
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions.Default
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isToxic) {
                    TextButton(onClick = onRephrase) {
                        Text("Rephrase", color = MaterialTheme.colorScheme.onSurface)
                    }
                } else {
                    Spacer(Modifier.size(1.dp))
                }

                // Send button — visually adapts; on toxic, opens override dialog
                // rather than being fully disabled (so users still have a path forward).
                val canSend = draft.isNotBlank()
                val sendBg by animateColorAsState(
                    targetValue = when {
                        !canSend -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        isToxic  -> AccentToxic
                        else     -> MaterialTheme.colorScheme.onSurface
                    },
                    animationSpec = tween(220), label = "send-bg"
                )
                val sendFg = if (isToxic) Color.White else MaterialTheme.colorScheme.background
                IconButton(
                    enabled = canSend,
                    onClick = onSend,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(sendBg)
                        .semantics { contentDescription = "Send button" }
                ) {
                    Icon(
                        imageVector = if (isToxic) Icons.Filled.WarningAmber else Icons.Filled.Send,
                        contentDescription = null,
                        tint = sendFg
                    )
                }
            }
        }
    }
}
