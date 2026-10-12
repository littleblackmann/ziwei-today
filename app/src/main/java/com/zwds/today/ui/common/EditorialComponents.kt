package com.zwds.today.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zwds.today.ui.theme.*

@Composable
fun StarHero(eyebrow: String, title: String, subtitle: String) {
    ProgressiveReveal {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF182248), Color(0xFF302451), Color(0xFF123748))))
        .border(1.dp, Brush.linearGradient(listOf(NebulaViolet.copy(alpha = .7f), NebulaCyan.copy(alpha = .35f))), RoundedCornerShape(28.dp))) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width * .83f, size.height * .47f)
            drawCircle(Brush.radialGradient(listOf(NebulaViolet.copy(alpha = .23f), Color.Transparent), center,
                size.width * .48f), size.width * .48f, center)
            repeat(3) { orbit ->
                val radius = size.width * (.20f + orbit * .10f)
                drawOval(NebulaCyan.copy(alpha = .10f - orbit * .02f),
                    topLeft = Offset(center.x - radius, center.y - radius * .63f),
                    size = Size(radius * 2, radius * 1.26f), style = Stroke(1.dp.toPx()))
            }
            val points = listOf(.66f to .21f, .81f to .13f, .91f to .30f, .77f to .46f, .91f to .65f)
                .map { Offset(size.width * it.first, size.height * it.second) }
            points.zipWithNext().forEach { (a, b) -> drawLine(NebulaCyan.copy(alpha = .25f), a, b, 1.dp.toPx()) }
            points.forEachIndexed { i, p ->
                drawCircle(NebulaCyan.copy(alpha = .10f), if (i == 1) 9.dp.toPx() else 5.dp.toPx(), p)
                drawCircle(HeroPaper.copy(alpha = if (i == 1) .85f else .5f), if (i == 1) 2.5.dp.toPx() else 1.5.dp.toPx(), p)
            }
        }
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = .07f),
                border = BorderStroke(1.dp, NebulaCyan.copy(alpha = .22f))) {
                Text(eyebrow, Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium, color = Color(0xFFC6EDF8))
            }
            Text(title, style = MaterialTheme.typography.headlineLarge, color = HeroPaper, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFCCD3EC))
        }
    }
    }
}
@Composable
fun SectionTitle(title: String, detail: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.width(3.dp).height(18.dp).background(
                Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)), RoundedCornerShape(2.dp)))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
@Composable
fun EditorialPanel(content: @Composable ColumnScope.() -> Unit) {
    ProgressiveReveal(Modifier.fillMaxWidth(), index = 1) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface, border = cosmicBorder()) {
        Column(Modifier.cosmicPanelBackground().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
    }
}
@Composable
fun MenuTile(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, revealIndex: Int = 2) {
    ProgressiveReveal(modifier.fillMaxWidth(), index = revealIndex) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface, border = cosmicBorder()) {
        Row(Modifier.cosmicPanelBackground().padding(18.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, null, Modifier.padding(12.dp).size(24.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
        }
    }
    }
}
