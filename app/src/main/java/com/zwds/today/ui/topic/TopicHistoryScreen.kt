package com.zwds.today.ui.topic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zwds.today.data.repository.TopicHistoryRecord
import com.zwds.today.di.ZwdsViewModelFactory
import com.zwds.today.domain.model.FortuneTopic
import com.zwds.today.ui.common.*
import java.time.LocalDate

@Composable
fun TopicHistoryScreen(memberId: Long, onBack: () -> Unit,
    onOpenRecord: (Long, FortuneTopic, LocalDate) -> Unit) {
    val vm: TopicHistoryViewModel = viewModel(factory = ZwdsViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(memberId) { vm.load(memberId) }
    var pendingDelete by remember { mutableStateOf<TopicHistoryRecord?>(null) }
    ZwdsScaffold("解讀紀錄", onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { SectionTitle("回看走過的每一天", if (state.memberName.isBlank()) "您的每日提醒，都收在這裡。" else "${state.memberName}的每日提醒，都收在這裡。") }
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            else if (state.error != null) item { Text(state.error!!, color = MaterialTheme.colorScheme.error) }
            else if (state.records.isEmpty()) item {
                EditorialPanel {
                    Icon(Icons.Default.History, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.secondary)
                    SectionTitle("第一份紀錄，從今天開始", "回到首頁選擇主題，完成解讀後會自動保存在這裡。")
                    BigOutlinedButton("返回首頁", onBack)
                }
            }
            items(state.records, key = { it.id }) { record ->
                Surface(onClick = { onOpenRecord(record.memberId, record.topic, record.date) },
                    shape = MaterialTheme.shapes.large, color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface, border = cosmicBorder()) {
                    Column(Modifier.cosmicPanelBackground().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(record.topic.symbol(), null, tint = MaterialTheme.colorScheme.secondary)
                            Column(Modifier.weight(1f)) {
                                Text(record.topic.cnName, style = MaterialTheme.typography.titleMedium)
                                Text(record.date.toString(), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton({ pendingDelete = record }) { Icon(Icons.Default.DeleteOutline, "刪除這筆解讀") }
                        }
                        Text(record.title, style = MaterialTheme.typography.titleLarge)
                        if (record.focusSummary.isNotBlank())
                            Text(record.focusSummary, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    pendingDelete?.let { record ->
        AlertDialog(onDismissRequest = { pendingDelete = null }, title = { Text("刪除這筆解讀？") },
            text = { Text("${record.date} · ${record.topic.cnName}\n再次產生此份解讀將使用會員額度。") },
            confirmButton = { TextButton({ vm.delete(record.id); pendingDelete = null }) { Text("刪除") } },
            dismissButton = { TextButton({ pendingDelete = null }) { Text("取消") } })
    }
}
