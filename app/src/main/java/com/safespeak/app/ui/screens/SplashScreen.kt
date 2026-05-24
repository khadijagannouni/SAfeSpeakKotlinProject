package com.safespeak.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safespeak.app.ui.theme.Ink800
import com.safespeak.app.ui.theme.Ink900
import com.safespeak.app.ui.theme.Lime
import com.safespeak.app.ui.theme.LimeDim
import com.safespeak.app.ui.theme.Paper
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {

    // Three-stage entry: shield draws → wordmark fades → tagline slides.
    val shieldProgress = remember { Animatable(0f) }
    val wordmarkAlpha  = remember { Animatable(0f) }
    val taglineOffset  = remember { Animatable(20f) }

    val infinite = rememberInfiniteTransition(label = "splash-loop")
    val pulse by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val orbit by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing)
        ),
        label = "orbit"
    )

    LaunchedEffect(Unit) {
        shieldProgress.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
        wordmarkAlpha.animateTo(1f, animationSpec = tween(500))
        taglineOffset.animateTo(0f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        delay(900)
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Ink800, Ink900),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        // Decorative orbiting dots — subtle motion that hints at "scanning".
        Box(
            Modifier
                .size(260.dp)
                .rotate(orbit),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(Modifier.size(260.dp)) {
                val r = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                for (i in 0 until 24) {
                    val angle = (i * 15f) * Math.PI.toFloat() / 180f
                    val x = center.x + r * 0.95f * kotlin.math.cos(angle)
                    val y = center.y + r * 0.95f * kotlin.math.sin(angle)
                    drawCircle(
                        color = if (i % 6 == 0) Lime else LimeDim.copy(alpha = 0.25f),
                        radius = if (i % 6 == 0) 3f else 1.5f,
                        center = Offset(x, y)
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated shield mark
            androidx.compose.foundation.Canvas(
                Modifier
                    .size(108.dp)
                    .scale(pulse)
            ) {
                val w = size.width
                val h = size.height
                val shield = Path().apply {
                    moveTo(w * 0.5f, h * 0.10f)
                    lineTo(w * 0.85f, h * 0.25f)
                    lineTo(w * 0.85f, h * 0.55f)
                    cubicTo(
                        w * 0.85f, h * 0.78f,
                        w * 0.70f, h * 0.90f,
                        w * 0.5f, h * 0.94f
                    )
                    cubicTo(
                        w * 0.30f, h * 0.90f,
                        w * 0.15f, h * 0.78f,
                        w * 0.15f, h * 0.55f
                    )
                    lineTo(w * 0.15f, h * 0.25f)
                    close()
                }

                // Filled lime shield
                drawPath(shield, color = Lime.copy(alpha = shieldProgress.value))

                // Checkmark – drawn in proportion to shieldProgress
                val checkProgress = shieldProgress.value
                if (checkProgress > 0.3f) {
                    val check = Path().apply {
                        moveTo(w * 0.36f, h * 0.55f)
                        lineTo(w * 0.47f, h * 0.66f)
                        lineTo(w * 0.66f, h * 0.42f)
                    }
                    drawPath(
                        check,
                        color = Ink900,
                        style = Stroke(
                            width = 10f * checkProgress,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Wordmark — serif, large, with a single lime period.
            androidx.compose.foundation.layout.Row(modifier = Modifier.alpha(wordmarkAlpha.value)) {
                Text(
                    text = "SafeSpeak",
                    color = Paper,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                )
                Text(
                    text = ".",
                    color = Lime,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black
                    )
                )
            }

            Spacer(Modifier.height(8.dp))

            // Tagline with mono accent line
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(wordmarkAlpha.value)
            ) {
                Box(
                    Modifier
                        .padding(bottom = 12.dp)
                        .width(40.dp)
                        .height(2.dp)
                        .background(Lime)
                )
                Text(
                    text = "REAL-TIME · ON-DEVICE · PRIVATE",
                    color = Paper.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp
                )
            }
        }

        // Footer
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "v1.0",
                color = Paper.copy(alpha = 0.4f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
