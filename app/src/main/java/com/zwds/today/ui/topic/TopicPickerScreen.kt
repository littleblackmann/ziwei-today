package com.zwds.today.ui.topic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zwds.today.di.ZwdsViewModelFactory
import com.zwds.today.domain.model.FortuneTopic
import com.zwds.today.ui.common.*

fun FortuneTopic.symbol(): ImageVector = when (this) {
    FortuneTopic.OVERVIEW -> Icons.Default.WbTwilight
    FortuneTopic.HEALTH -> Icons.Default.Spa
    FortuneTopic.WEALTH -> Icons.Default.AccountBalanceWallet
    FortuneTopic.CAREER -> Icons.Default.WorkOutline
    FortuneTopic.LOVE -> Icons.Default.FavoriteBorder
    FortuneTopic.FRIENDS -> Icons.Default.PeopleOutline
    FortuneTopic.TRAVEL -> Icons.Default.Explore
    FortuneTopic.MOOD -> Icons.Default.NightsStay
}

@Composable
fun TopicPickerScreen(memberId: Long, onBack: () -> Unit, onTopicSelected: (FortuneTopic) -> Unit) {
    val vm: TopicPickerViewModel = viewModel(factory = ZwdsViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(memberId) { vm.load(memberId) }
    ZwdsScaffold(if (state.memberName.isBlank()) "今日解讀" else "${state.memberName}的今日解讀", onBack) { padding ->
        LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(bottom = 16.dp)) {
                    SectionTitle("今天，想了解什麼？", "選一個在意的主題，聽聽星曜給您的提醒。")
                }
            }
            if (state.error != null) item(span = { GridItemSpan(maxLineSpan) }) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            } else itemsIndexed(FortuneTopic.entries, key = { _, topic -> topic.name }) { index, topic ->
                val accent = when (index % 3) {
                    0 -> MaterialTheme.colorScheme.primary
                    1 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.tertiary
                }
                ProgressiveReveal(Modifier.fillMaxWidth(), index = index) {
                Surface(onClick = { onTopicSelected(topic) }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp), color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface, border = cosmicBorder()) {
                    Column(Modifier.cosmicPanelBackground().padding(18.dp).heightIn(min = 150.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).background(Brush.linearGradient(listOf(accent.copy(alpha = .20f), accent.copy(alpha = .06f))), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center) {
                                Icon(topic.symbol(), null, Modifier.size(26.dp), tint = accent)
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp), tint = accent.copy(alpha = .7f))
                        }
                        Text(topic.cnName, style = MaterialTheme.typography.titleMedium)
                        Text(topic.description, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                }
            }
        }
    }
}
