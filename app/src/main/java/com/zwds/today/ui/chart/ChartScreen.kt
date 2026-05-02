package com.zwds.today.ui.chart

import androidx.compose.runtime.Composable
import com.zwds.today.ui.common.ZwdsScaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zwds.today.astro.chart.ChartBuilder
import com.zwds.today.data.repository.gender
import com.zwds.today.data.repository.toSolarBirth
import com.zwds.today.di.ServiceLocator
import com.zwds.today.domain.model.ChartResult
import com.zwds.today.domain.model.EarthlyBranch
import com.zwds.today.domain.model.PlacedStar
import com.zwds.today.domain.model.StarCategory
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * 完整十二宮命盤
 *
 * 採傳統「正方形十二宮」佈局，但簡化為 4×4 grid，外圈 12 格為宮位、中心 4 格為命主資訊。
 * V1：以「主帳號」家人為對象。多家人切換留待後續加入。
 */
@Composable
fun ChartScreen(onBack: () -> Unit) {
    var chart by remember { mutableStateOf<ChartResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val primary = ServiceLocator.familyRepository.getPrimary()
            if (primary == null) {
                error = "尚未設定主帳號家人"
                return@LaunchedEffect
            }
            val school = ServiceLocator.userPreferences.ziHourSchool.first()
            chart = ChartBuilder.build(
                solarBirth = primary.toSolarBirth(),
                gender = primary.gender(),
                targetDate = LocalDate.now(),
                ziHourSchool = school
            )
        } catch (e: Exception) {
            error = e.message ?: "命盤計算失敗"
        }
    }

    ZwdsScaffold(title = "完整命盤", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            error?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            chart?.let { c ->
                ChartHeader(c)
                Spacer(Modifier.height(8.dp))
                TwelvePalaceGrid(c)
            }
        }
    }
}

@Composable
private fun ChartHeader(c: ChartResult) {
    val lunar = c.birthLunar
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("農曆 ${lunar.year} 年 ${lunar.month} 月 ${lunar.day} 日" +
                if (lunar.isLeapMonth) "（閏月）" else "",
                style = MaterialTheme.typography.titleMedium)
            Text("命宮：${c.lifePalaceBranch.cnName}　身宮：${c.bodyPalaceBranch.cnName}　${c.fiveElement.cnName}",
                style = MaterialTheme.typography.bodyMedium)
            Text("年柱 ${c.yearStem.cnName}${c.yearBranch.cnName}　月柱 ${c.monthStem.cnName}${c.monthBranch.cnName}",
                style = MaterialTheme.typography.bodyMedium)
            Text("日柱 ${c.dayStem.cnName}${c.dayBranch.cnName}　時支 ${c.hourBranch.cnName}",
                style = MaterialTheme.typography.bodyMedium)
            Text("流日命宮：${c.flowDayPalaceBranch.cnName}（今日）",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * 十二宮 4x4 排列（傳統盤型）
 *
 * 標準紫微盤型方位（地支對應 4×4 grid，從左上角逆時針）：
 *   巳(5)  午(6)  未(7)  申(8)
 *   辰(4)  [中心  -  -]   酉(9)
 *   卯(3)  [-     -  -]   戌(10)
 *   寅(2)  丑(1)  子(0)  亥(11)
 */
@Composable
private fun TwelvePalaceGrid(c: ChartResult) {
    // 4 行
    val rows = listOf(
        listOf(EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI, EarthlyBranch.SHEN),
        listOf(EarthlyBranch.CHEN, null, null, EarthlyBranch.YOU),
        listOf(EarthlyBranch.MAO, null, null, EarthlyBranch.XU),
        listOf(EarthlyBranch.YIN, EarthlyBranch.CHOU, EarthlyBranch.ZI, EarthlyBranch.HAI)
    )
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEachIndexed { rowIdx, row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEachIndexed { colIdx, branch ->
                    when {
                        branch != null ->
                            PalaceCell(c, branch, modifier = Modifier.weight(1f).height(160.dp))
                        // 中心 2×2 區域：row1 col1 放 CenterCard（weight(2f) 涵蓋 2 欄）
                        rowIdx == 1 && colIdx == 1 ->
                            CenterCard(c, modifier = Modifier.weight(2f).height(160.dp))
                        rowIdx == 1 && colIdx == 2 -> { /* 已被 CenterCard weight(2f) 涵蓋 */ }
                        // row2 中心：放 Spacer 維持佈局比例
                        rowIdx == 2 && colIdx == 1 ->
                            Spacer(modifier = Modifier.weight(2f).height(160.dp))
                        rowIdx == 2 && colIdx == 2 -> { /* 已被上方 Spacer weight(2f) 涵蓋 */ }
                        else ->
                            Spacer(modifier = Modifier.weight(1f).height(160.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PalaceCell(c: ChartResult, branch: EarthlyBranch, modifier: Modifier = Modifier) {
    val palace = c.palaceAt(branch)
    val isLife = branch == c.lifePalaceBranch
    val isFlowDay = branch == c.flowDayPalaceBranch
    val borderColor = when {
        isFlowDay -> MaterialTheme.colorScheme.primary
        isLife -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }
    val mainStarsText = palace.stars
        .filter { it.star.category == StarCategory.MAIN }
        .joinToString("\n") { it.dictKey }
    val otherStarsText = palace.stars
        .filter { it.star.category != StarCategory.MAIN }
        .joinToString("\n") { it.dictKey }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(
                width = if (isFlowDay || isLife) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(6.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    branch.cnName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    palace.type.cnName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isFlowDay) Text("今日", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            if (mainStarsText.isNotEmpty()) {
                Text(
                    mainStarsText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (otherStarsText.isNotEmpty()) {
                Text(
                    otherStarsText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CenterCard(c: ChartResult, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("命主資訊", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text("命宮 ${c.lifePalaceBranch.cnName}", style = MaterialTheme.typography.bodySmall)
            Text("身宮 ${c.bodyPalaceBranch.cnName}", style = MaterialTheme.typography.bodySmall)
            Text(c.fiveElement.cnName, style = MaterialTheme.typography.bodySmall)
            Text("年 ${c.yearStem.cnName}${c.yearBranch.cnName}", style = MaterialTheme.typography.bodySmall)
            Text("流日 ${c.flowDayPalaceBranch.cnName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
