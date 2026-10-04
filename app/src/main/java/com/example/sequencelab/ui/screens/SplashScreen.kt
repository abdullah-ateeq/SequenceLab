package com.example.sequencelab.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val SPLASH_MS = 1300
private val NODE_VALUES = listOf("2", "4", "6", "8")

/** Nodes appear, connect into 2 → 4 → 6, the next term 8 appears, then the title fades in. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(SPLASH_MS, easing = LinearEasing))
        onFinished()
    }
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val nodeText = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .semantics { contentDescription = "SequenceLab" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(width = 260.dp, height = 64.dp)) {
                val t = progress.value
                val radius = 20.dp.toPx()
                val gap = (size.width - radius * 2) / (NODE_VALUES.size - 1)
                val centers = NODE_VALUES.indices.map { Offset(radius + gap * it, size.height / 2) }
                // Stage timings as fractions of the whole animation.
                fun window(start: Float, length: Float) = ((t - start) / length).coerceIn(0f, 1f)
                val nodeIn = listOf(window(0f, 0.15f), window(0.1f, 0.15f), window(0.2f, 0.15f), window(0.55f, 0.15f))
                val lineIn = listOf(window(0.25f, 0.12f), window(0.35f, 0.12f), window(0.45f, 0.12f))

                lineIn.forEachIndexed { i, f ->
                    if (f > 0f) {
                        val from = centers[i] + Offset(radius, 0f)
                        val to = centers[i + 1] - Offset(radius, 0f)
                        drawLine(
                            color = if (i == lineIn.lastIndex) colors.primary else colors.outline,
                            start = from,
                            end = from + (to - from) * f,
                            strokeWidth = 1.5.dp.toPx(),
                        )
                    }
                }
                centers.forEachIndexed { i, center ->
                    val f = nodeIn[i]
                    if (f <= 0f) return@forEachIndexed
                    val isNext = i == centers.lastIndex
                    val tint = if (isNext) colors.primary else colors.onSurface
                    if (isNext) drawCircle(colors.primaryContainer.copy(alpha = f), radius * f, center)
                    drawCircle(tint.copy(alpha = f), radius * f, center, style = Stroke(1.5.dp.toPx()))
                    val layout = measurer.measure(NODE_VALUES[i], nodeText.copy(color = tint.copy(alpha = f)))
                    drawText(
                        layout,
                        topLeft = center - Offset(layout.size.width / 2f, layout.size.height / 2f),
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            val titleAlpha = ((progress.value - 0.65f) / 0.3f).coerceIn(0f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = titleAlpha },
            ) {
                Text(
                    "SEQUENCELAB",
                    style = MaterialTheme.typography.headlineSmall,
                    letterSpacing = 6.sp,
                    color = colors.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Identify & generate number patterns",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}
