package com.zwds.today.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.zwds.today.ui.theme.*

/** One short entrance, following Compose's system animation duration scale. No permanent render loop. */
@Composable
fun ProgressiveReveal(modifier: Modifier = Modifier, index: Int = 0, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(340, delayMillis = index.coerceIn(0, 5) * 45, easing = FastOutSlowInEasing))
    }
    Box(modifier.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 12.dp.toPx()
    }) { content() }
}

@Composable
fun cosmicPanelBrush(): Brush {
    val colors = MaterialTheme.colorScheme
    val dark = LocalCosmicDark.current
    return remember(colors, dark) {
        Brush.linearGradient(listOf(
            if (dark) Color(0xFF1D2340) else Color(0xFFFEFDFF),
            colors.surface,
            if (dark) Color(0xFF121C30) else Color(0xFFF0F6FF)
        ))
    }
}

@Composable
fun cosmicBorder(): BorderStroke {
    val dark = LocalCosmicDark.current
    return remember(dark) {
        BorderStroke(1.dp, Brush.linearGradient(listOf(
            if (dark) NebulaViolet.copy(alpha = .55f) else Color(0xFFD1C9F2),
            if (dark) NebulaCyan.copy(alpha = .18f) else Color(0xFFD8E9F3),
            if (dark) Color(0xFF29344F) else Color(0xFFDEE3F2)
        )))
    }
}

@Composable
fun Modifier.cosmicPanelBackground() = background(cosmicPanelBrush(), RoundedCornerShape(24.dp))

@Composable
fun CosmicBackdrop(modifier: Modifier = Modifier) {
    val dark = LocalCosmicDark.current
    val base = MaterialTheme.colorScheme.background
    Canvas(modifier.drawWithCache {
        val topGlow = Brush.radialGradient(
            listOf((if (dark) NebulaViolet else Color(0xFFBFAAFF)).copy(alpha = if (dark) .17f else .24f), Color.Transparent),
            center = Offset(size.width * .88f, size.height * .12f), radius = size.width * .85f)
        val sideGlow = Brush.radialGradient(
            listOf(NebulaCyan.copy(alpha = if (dark) .07f else .12f), Color.Transparent),
            center = Offset(0f, size.height * .65f), radius = size.width * .75f)
        onDrawBehind {
            drawRect(base)
            drawRect(topGlow)
            drawRect(sideGlow)
            if (dark) repeat(24) { i ->
                drawCircle(Color(0xFFA7B8E8).copy(alpha = .13f), .8.dp.toPx(),
                    Offset(size.width * ((i * 37 % 101) / 101f), size.height * ((i * 53 % 103) / 103f)))
            }
        }
    }) { }
}
