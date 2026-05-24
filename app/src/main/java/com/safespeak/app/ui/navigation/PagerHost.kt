package com.safespeak.app.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safespeak.app.ui.screens.ChatScreen
import com.safespeak.app.ui.screens.ChatViewModel
import com.safespeak.app.ui.screens.HistoryScreen
import com.safespeak.app.ui.theme.Lime
import kotlinx.coroutines.launch

/**
 * Two-page horizontal pager hosting Compose and History screens.
 * Both pages share a single [ChatViewModel] instance so the History
 * screen sees every message the user has sent.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerHost() {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()
    // Hoist ChatViewModel at PagerHost level so both pages share the same instance.
    val sharedVm: ChatViewModel = viewModel()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PageIndicator(
            currentPage = pagerState.currentPage,
            onSelect = { idx -> scope.launch { pagerState.animateScrollToPage(idx) } }
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true
        ) { page ->
            when (page) {
                0 -> ChatScreen(viewModel = sharedVm)
                1 -> HistoryScreen(viewModel = sharedVm)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageIndicator(currentPage: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("Compose", "History")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEachIndexed { idx, label ->
            val selected = currentPage == idx
            val color by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.onSurface
                              else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                animationSpec = tween(220), label = "label-color"
            )
            val underlineWidth by animateDpAsState(
                targetValue = if (selected) 32.dp else 0.dp,
                animationSpec = tween(220), label = "underline"
            )
            // Make the whole label area a reliable tap target.
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(idx) }
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    color = color
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .width(underlineWidth)
                        .height(2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Lime)
                )
            }
        }
    }
}
