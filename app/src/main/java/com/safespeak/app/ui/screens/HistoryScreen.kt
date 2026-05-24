package com.safespeak.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safespeak.app.ui.components.HistoryRow
import com.safespeak.app.ui.theme.Lime

enum class HistoryFilter(val label: String) {
    ALL("All"),
    SAFE("Safe"),
    FLAGGED("Flagged"),
    OVERRIDDEN("Overridden")
}

@Composable
fun HistoryScreen(viewModel: ChatViewModel = viewModel()) {
    val all by viewModel.history.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }

    val filtered = remember(all, filter) {
        when (filter) {
            HistoryFilter.ALL        -> all
            HistoryFilter.SAFE       -> all.filter { !it.wasFlagged && !it.timedOut }
            HistoryFilter.FLAGGED    -> all.filter { it.wasFlagged }
            HistoryFilter.OVERRIDDEN -> all.filter { it.overridden }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(Lime)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "AUDIT TRAIL",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "History",
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 36.sp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${all.size} messages · stored on-device only",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }

        // Filter tabs
        FilterTabs(selected = filter, onSelect = { filter = it })

        Spacer(Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp)
            ) {
                items(filtered, key = { it.id }) { item ->
                    HistoryRow(item)
                }
                item { Spacer(Modifier.height(28.dp)) }
            }
        }
    }
}

@Composable
private fun FilterTabs(selected: HistoryFilter, onSelect: (HistoryFilter) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HistoryFilter.entries.forEach { f ->
            val isSelected = f == selected
            val bg by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface
                              else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(200), label = "tab-bg"
            )
            val fg by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.background
                              else MaterialTheme.colorScheme.onSurface,
                animationSpec = tween(200), label = "tab-fg"
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = if (isSelected) 0f else 0.35f),
                        RoundedCornerShape(50)
                    )
                    .clickable { onSelect(f) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    f.label.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = fg
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "No messages yet",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Swipe right to compose. Every message you send appears here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
