package com.safespeak.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.safespeak.app.domain.model.ToxicityCategory
import com.safespeak.app.ui.theme.AccentSafe
import com.safespeak.app.ui.theme.AccentToxic
import com.safespeak.app.ui.theme.AccentWarn

/** Live colour-graded toxicity bar (0.0 → 1.0). */
@Composable
fun ScoreBar(
    score: Float,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 6.dp
) {
    val animated by animateFloatAsState(
        targetValue = score.coerceIn(0f, 1f),
        animationSpec = tween(280),
        label = "score-anim"
    )
    val colour by animateColorAsState(
        targetValue = when {
            animated < 0.40f -> AccentSafe
            animated < 0.75f -> AccentWarn
            else             -> AccentToxic
        },
        animationSpec = tween(280),
        label = "color-anim"
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(colour)
        )
    }
}

/** Pill labels for detected toxicity categories. */
@Composable
fun CategoryChips(categories: List<ToxicityCategory>) {
    if (categories.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        categories.take(4).forEach { c ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AccentToxic.copy(alpha = 0.12f))
                    .border(1.dp, AccentToxic.copy(alpha = 0.35f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    c.label.uppercase(),
                    color = AccentToxic,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/** Status dot used in the history list (and headers). */
@Composable
fun StatusDot(
    color: Color,
    size: androidx.compose.ui.unit.Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

/** Compact key/value chip for displaying latency or score in headers. */
@Composable
fun StatPill(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
