package com.zwds.today.ui.topic

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zwds.today.data.repository.TopicHistoryRecord
import com.zwds.today.di.ZwdsViewModelFactory
import com.zwds.today.domain.model.FortuneLevel
import com.zwds.today.domain.model.FortuneTopic
import com.zwds.today.ui.theme.FortuneBad
import com.zwds.today.ui.theme.FortuneCaution
import com.zwds.today.ui.theme.FortuneGood
import com.zwds.today.ui.theme.FortuneNeutral
import com.zwds.today.ui.theme.FortuneVeryGood
import com.zwds.today.ui.common.ZwdsScaffold
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TopicHistoryScreen(
    memberId: Long,
    onBack: () -> Unit,
    onOpenRecord: (memberId: Long, topic: FortuneTopic) -> Unit
) {
    val vm: TopicHistoryViewModel = viewModel(factory = ZwdsViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(memberId) { vm.load(memberId) }

    var pendingDelete by remember { mutableStateOf<TopicHistoryRecord?>(null) }
    val title = if (state.memberName.isNotBlank()) "${state.memberName}的查詢記錄" else "查詢記錄"

    ZwdsScaffold(title = title, onBack = onBack) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                state.error != null -> ErrorBlock(state.error!!)

                state.records.isEmpty() -> EmptyHistory()

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
                ) {
                    items(state.records, key = { it.id }) { record ->
                        HistoryItem(
                            record = record,
                            onClick = { onOpenRecord(record.memberId, record.topic) },
                            onDelete = { pendingDelete = record }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("刪除這筆記錄？") },
            text = {
                Text(
                    "${record.date} ${record.topic.cnName}：${record.title}\n" +
                        "刪除後若再次查詢同一天同一主題，會重新呼叫 OpenAI（會再消耗一點點額度）。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(record.id)
                    pendingDelete = null
                }) { Text("刪除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun HistoryItem(
    record: TopicHistoryRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val titleColor = when (record.level) {
        FortuneLevel.VERY_GOOD -> FortuneVeryGood
        FortuneLevel.GOOD -> FortuneGood
        FortuneLevel.NEUTRAL -> FortuneNeutral
        FortuneLevel.CAUTION -> FortuneCaution
        FortuneLevel.BAD -> FortuneBad
    }
    val dateText = record.date.format(
        DateTimeFormatter.ofPattern("M 月 d 日 EEEE", Locale.TAIWAN)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(record.topic.emoji, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        record.topic.cnName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        dateText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(record.level.emoji, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        record.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = titleColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (record.focusSummary.isNotBlank()) {
                    Text(
                        record.focusSummary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "刪除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("還沒有查詢過任何主題", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "回到首頁，按「開始今日詢問」就可以為您撰寫第一份運勢。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorBlock(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

