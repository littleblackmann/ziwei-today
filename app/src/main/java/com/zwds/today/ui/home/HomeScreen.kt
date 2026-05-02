package com.zwds.today.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zwds.today.di.ZwdsViewModelFactory
import com.zwds.today.ui.common.BigOutlinedButton
import com.zwds.today.ui.common.BigPrimaryButton
import com.zwds.today.ui.common.ZwdsTopBar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToFamily: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToChart: () -> Unit,
    onStartTopicQuery: (memberId: Long) -> Unit,
    onOpenHistory: (memberId: Long) -> Unit,
    showFullChartEntry: Boolean
) {
    val vm: HomeViewModel = viewModel(factory = ZwdsViewModelFactory)
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ZwdsTopBar(
                title = "今日紫微",
                actions = {
                    IconButton(onClick = onNavigateToFamily, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Person, contentDescription = "家人管理")
                    }
                    IconButton(onClick = onNavigateToSettings, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Settings, contentDescription = "設定")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            DateBanner()

            if (state.members.isEmpty()) {
                EmptyState(onNavigateToFamily)
                return@Column
            }

            FamilyCarousel(
                members = state.members.map { it.name },
                selectedIndex = state.selectedIndex,
                onSelect = vm::selectIndex
            )

            IntroCard(name = state.members.getOrNull(state.selectedIndex)?.name ?: "")

            BigPrimaryButton(
                text = "開始今日詢問",
                onClick = {
                    val member = vm.selectedMember() ?: return@BigPrimaryButton
                    onStartTopicQuery(member.id)
                }
            )

            BigOutlinedButton(
                text = "查看詢問記錄",
                onClick = {
                    val member = vm.selectedMember() ?: return@BigOutlinedButton
                    onOpenHistory(member.id)
                }
            )

            if (showFullChartEntry) {
                BigPrimaryButton(
                    text = "查看完整命盤",
                    onClick = onNavigateToChart
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DateBanner() {
    val today = LocalDate.now()
    val text = today.format(DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日 EEEE", Locale.TAIWAN))
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
}

@Composable
private fun FamilyCarousel(
    members: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(members) { idx, name ->
            FamilyChip(
                name = name,
                selected = idx == selectedIndex,
                onClick = { onSelect(idx) }
            )
        }
    }
}

@Composable
private fun FamilyChip(name: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val bg = if (selected) colors.primary else colors.surfaceVariant
    val fg = if (selected) colors.onPrimary else colors.onSurfaceVariant
    Box(
        modifier = Modifier
            .heightIn(min = 56.dp)
            .widthIn(min = 80.dp)
            .background(bg, RoundedCornerShape(28.dp))
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .semantics {
                contentDescription = if (selected) "${name}（目前選中）" else "切換為 $name"
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            name,
            style = MaterialTheme.typography.titleLarge,
            color = fg,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun IntroCard(name: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (name.isNotBlank()) "$name，您好" else "您好",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "今天想了解哪一方面的運勢？按下方按鈕，挑選您想詢問的主題。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EmptyState(onAddFamily: () -> Unit) {
    Spacer(Modifier.height(48.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "歡迎",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "請先新增第一位家人，才能開始今日的運勢詢問。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            BigPrimaryButton(text = "新增第一位家人", onClick = onAddFamily)
        }
    }
}
