package com.zwds.today.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zwds.today.di.*
import com.zwds.today.ui.common.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(onNavigateToFamily: () -> Unit, onNavigateToSettings: () -> Unit,
    onNavigateToAccount: () -> Unit, onNavigateToChart: () -> Unit,
    onStartTopicQuery: (Long) -> Unit, onOpenHistory: (Long) -> Unit, showFullChartEntry: Boolean) {
    val vm: HomeViewModel = viewModel(factory = ZwdsViewModelFactory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val account by ServiceLocator.accountRepository.state.collectAsStateWithLifecycle()
    val member = state.members.getOrNull(state.selectedIndex)
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("M 月 d 日 · EEEE", Locale.TRADITIONAL_CHINESE))

    ZwdsScaffold("今日紫微", actions = {
        IconButton(onNavigateToFamily) { Icon(Icons.Default.PeopleOutline, "家人管理") }
        IconButton(onNavigateToSettings) { Icon(Icons.Default.Tune, "設定") }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            StarHero(date, if (member == null) "你的今日，\n正在展開。" else "${member.name}，\n探索新可能。",
                "讀懂星曜，找到自己的節奏。")
            if (member == null) {
                EditorialPanel {
                    SectionTitle("從您與家人開始", "新增生日與時辰，建立屬於自己的每日解讀。")
                    BigPrimaryButton("新增第一位家人", onNavigateToFamily, leadingIcon = Icons.Default.Add)
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("今天為誰解讀", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onNavigateToFamily) { Text("管理家人") }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(state.members, key = { _, item -> item.id }) { index, item ->
                        FilterChip(selected = index == state.selectedIndex, onClick = { vm.selectIndex(index) },
                            label = { Text(item.name, style = MaterialTheme.typography.titleMedium) },
                            modifier = Modifier.heightIn(min = 48.dp),
                            leadingIcon = if (index == state.selectedIndex) ({ Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }) else null)
                    }
                }
                EditorialPanel {
                    Text("留給今天的一個提問", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    SectionTitle("今日運勢", "工作、感情、財運，從在意的主題開始。")
                    BigPrimaryButton("開始今日解讀", { onStartTopicQuery(member.id) }, leadingIcon = Icons.Default.AutoAwesome)
                }
                MenuTile("解讀紀錄", "回看過去的提醒與每天的變化", Icons.Default.History, { onOpenHistory(member.id) })
                if (showFullChartEntry)
                    MenuTile("完整命盤", "查看主家人的十二宮與星曜", Icons.Default.GridView, onNavigateToChart, revealIndex = 3)
            }
            MenuTile("我的帳號", when {
                account.signingIn -> "登入進行中 · 返回查看進度"
                account.serviceReady -> "帳號已連線 · 可以開始解讀"
                account.connected -> "帳號已登入 · 查看解讀權限"
                else -> "連接自己的會員，開啟個人化解讀"
            }, Icons.Default.AccountCircle, onNavigateToAccount, revealIndex = 4)
            Text("星曜作為提醒，日子由自己選擇。", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(8.dp))
        }
    }
}
