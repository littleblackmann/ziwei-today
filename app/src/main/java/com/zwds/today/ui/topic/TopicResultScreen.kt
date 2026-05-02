package com.zwds.today.ui.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zwds.today.di.ZwdsViewModelFactory
import com.zwds.today.domain.model.FortuneLevel
import com.zwds.today.domain.model.FortuneTopic
import com.zwds.today.domain.model.TopicFortune
import com.zwds.today.ui.common.BigOutlinedButton
import com.zwds.today.ui.common.BigPrimaryButton
import com.zwds.today.ui.common.ZwdsScaffold
import com.zwds.today.ui.theme.FortuneBad
import com.zwds.today.ui.theme.FortuneCaution
import com.zwds.today.ui.theme.FortuneGood
import com.zwds.today.ui.theme.FortuneNeutral
import com.zwds.today.ui.theme.FortuneVeryGood

@Composable
fun TopicResultScreen(
    memberId: Long,
    topic: FortuneTopic,
    onBack: () -> Unit,
    onOpenApiKeySettings: () -> Unit
) {
    val vm: TopicResultViewModel = viewModel(factory = ZwdsViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(memberId, topic) { vm.load(memberId, topic) }

    ZwdsScaffold(title = topic.cnName, onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            when {
                state.loading -> LoadingCard()
                state.error != null -> ErrorContent(
                    error = state.error!!,
                    onRetry = vm::retry,
                    onOpenApiKeySettings = onOpenApiKeySettings
                )
                state.fortune != null -> {
                    ResultCard(state.fortune!!)
                    BigOutlinedButton(text = "重新產生（會再呼叫 OpenAI）", onClick = vm::regenerate)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(
                "AI 命理師正在為您撰寫運勢，請稍候 10 秒鐘...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: TopicResultError,
    onRetry: () -> Unit,
    onOpenApiKeySettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (error) {
                is TopicResultError.MissingApiKey -> {
                    Text(
                        "尚未設定 OpenAI API Key",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "本 App 透過 OpenAI 為您撰寫運勢。請先到「設定 → AI 設定」輸入您的 API Key。" +
                            "如還沒有，可在那裡點選「如何申請」依步驟註冊。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    BigPrimaryButton(text = "前往 AI 設定", onClick = onOpenApiKeySettings)
                }

                is TopicResultError.Network -> {
                    Text(
                        "OpenAI 呼叫失敗",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        error.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        "可能原因：API Key 無效、餘額不足、或網路不穩。請檢查後重試。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    BigPrimaryButton(text = "重試", onClick = onRetry)
                    BigOutlinedButton(text = "前往 AI 設定", onClick = onOpenApiKeySettings)
                }

                is TopicResultError.Unknown -> {
                    Text(
                        "載入失敗",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        error.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    BigPrimaryButton(text = "重試", onClick = onRetry)
                }
            }
        }
    }
}

@Composable
private fun ResultCard(fortune: TopicFortune) {
    val titleColor = when (fortune.level) {
        FortuneLevel.VERY_GOOD -> FortuneVeryGood
        FortuneLevel.GOOD -> FortuneGood
        FortuneLevel.NEUTRAL -> FortuneNeutral
        FortuneLevel.CAUTION -> FortuneCaution
        FortuneLevel.BAD -> FortuneBad
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "${fortune.familyMemberName}・${fortune.topic.cnName}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(fortune.level.emoji, style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.size(12.dp))
                Text(
                    fortune.title,
                    style = MaterialTheme.typography.displayMedium,
                    color = titleColor,
                    fontWeight = FontWeight.Black
                )
            }

            SummaryBanner(fortune.focusSummary)

            QuickFacts(fortune)

            if (fortune.tags.isNotEmpty()) {
                SectionBlock(title = "今日重點") {
                    TagGroup(fortune.tags)
                }
            }

            SectionBlock(title = "詳細解讀") {
                adviceParagraphs(fortune.advice).forEach { paragraph ->
                    Text(
                        paragraph,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            SectionBlock(title = "命理依據") {
                Text(
                    "對應宮位：${fortune.topic.palaceType.flowName}（${fortune.flowPalaceBranch.cnName}）",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (fortune.flowPalaceStars.isNotEmpty()) {
                        "今日駐守：${fortune.flowPalaceStars.joinToString("、") { p -> if (p.transformation != null) "${p.star.cnName}${p.transformation.cnName}" else p.star.cnName }}"
                    } else {
                        "今日此宮為空宮，偏向靠整體流日氣勢與周遭人事帶動。"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryBanner(summary: String) {
    if (summary.isBlank()) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.26f),
                RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "今天先看這一句",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                summary,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QuickFacts(fortune: TopicFortune) {
    val summaryItems = listOf(
        "今日等級" to fortune.level.cnName,
        "適合做的事" to fortune.tags.getOrNull(0).orEmpty(),
        "最要留意" to fortune.tags.getOrNull(1).orEmpty()
    ).filter { it.second.isNotBlank() }

    if (summaryItems.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        summaryItems.forEach { (label, value) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionBlock(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                content()
            }
        )
    }
}

@Composable
private fun TagGroup(tags: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.take(4).chunked(2).forEach { rowTags ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowTags.forEach { tag -> TagChip(tag) }
            }
        }
    }
}

@Composable
private fun TagChip(text: String) {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                RoundedCornerShape(12.dp)
            )
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun adviceParagraphs(advice: String): List<String> {
    val normalized = advice
        .replace("\r\n", "\n")
        .trim()
    if (normalized.isBlank()) return emptyList()

    val explicitParagraphs = normalized
        .split("\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (explicitParagraphs.size >= 2) return explicitParagraphs

    val sentences = Regex("(?<=[。！？])")
        .split(normalized)
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (sentences.size <= 2) return listOf(normalized)

    if (sentences.size >= 6) {
        val chunkSize = kotlin.math.ceil(sentences.size / 3.0).toInt()
        return sentences
            .chunked(chunkSize)
            .map { it.joinToString(" ") }
            .filter { it.isNotBlank() }
    }

    val midpoint = (sentences.size + 1) / 2
    return listOf(
        sentences.take(midpoint).joinToString(" "),
        sentences.drop(midpoint).joinToString(" ")
    ).filter { it.isNotBlank() }
}
