package com.zwds.today.ui.settings

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zwds.today.data.prefs.ThemeMode
import com.zwds.today.di.ZwdsViewModelFactory
import com.zwds.today.domain.model.ZiHourSchool
import com.zwds.today.ui.common.BigOutlinedButton
import com.zwds.today.ui.common.ZwdsScaffold
import com.zwds.today.ui.theme.FontScale

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onShowDisclaimer: () -> Unit,
    onShowApiKeyTutorial: () -> Unit
) {
    val vm: SettingsViewModel = viewModel(factory = ZwdsViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()

    var apiKeyDraft by remember { mutableStateOf(state.openAiApiKey) }
    var showKey by remember { mutableStateOf(false) }
    LaunchedEffect(state.openAiApiKey) {
        if (apiKeyDraft != state.openAiApiKey) apiKeyDraft = state.openAiApiKey
    }

    ZwdsScaffold(title = "設定", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // AI 設定（最重要，放最上）
            SectionTitle("AI 設定")
            Text(
                if (state.openAiApiKey.isNotBlank())
                    "API Key 已設定，今日紫微會以 GPT-5.4-mini 為您撰寫個人化運勢解讀。"
                else
                    "本 App 透過 OpenAI GPT-5.4-mini 為您撰寫運勢解讀，必須先設定您自己的 OpenAI API Key 才能使用。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            SettingItem("OpenAI API Key") {
                OutlinedTextField(
                    value = apiKeyDraft,
                    onValueChange = { apiKeyDraft = it },
                    placeholder = { Text("sk-...") },
                    singleLine = true,
                    visualTransformation =
                        if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showKey = !showKey }) {
                        Text(if (showKey) "隱藏" else "顯示")
                    }
                    TextButton(onClick = {
                        apiKeyDraft = ""
                        vm.setOpenAiApiKey("")
                    }) { Text("清除") }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = { vm.setOpenAiApiKey(apiKeyDraft) },
                        enabled = apiKeyDraft != state.openAiApiKey
                    ) { Text("儲存") }
                }
            }
            BigOutlinedButton(
                text = "如何申請 OpenAI API Key？",
                onClick = onShowApiKeyTutorial
            )

            HorizontalDivider()

            // 文字顯示
            SectionTitle("文字顯示")
            SettingItem("字體大小") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FontScale.entries.forEach { scale ->
                        OptionChip(
                            label = scale.displayName,
                            selected = state.fontScale == scale,
                            onClick = { vm.setFontScale(scale) }
                        )
                    }
                }
            }

            SettingItem("色彩模式") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        OptionChip(
                            label = mode.cnName,
                            selected = state.themeMode == mode,
                            onClick = { vm.setThemeMode(mode) },
                            fullWidth = true
                        )
                    }
                }
            }

            HorizontalDivider()

            SectionTitle("進階")
            ToggleRow(
                title = "顯示完整命盤頁",
                subtitle = "在首頁顯示「查看完整命盤」按鈕",
                checked = state.showFullChart,
                onChange = vm::setShowFullChart
            )

            SettingItem("23 點出生（晚子時）歸屬") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionChip(
                        label = "晚子時歸明日（多數現代派）",
                        selected = state.ziHourSchool == ZiHourSchool.LATE_BELONGS_TO_NEXT_DAY,
                        onClick = { vm.setZiHourSchool(ZiHourSchool.LATE_BELONGS_TO_NEXT_DAY) },
                        fullWidth = true
                    )
                    OptionChip(
                        label = "晚子時歸今日（傳統派）",
                        selected = state.ziHourSchool == ZiHourSchool.LATE_BELONGS_TO_SAME_DAY,
                        onClick = { vm.setZiHourSchool(ZiHourSchool.LATE_BELONGS_TO_SAME_DAY) },
                        fullWidth = true
                    )
                }
            }

            HorizontalDivider()

            SectionTitle("關於")
            BigOutlinedButton(text = "查看免責聲明", onClick = onShowDisclaimer)

            Text(
                "今日紫微 v1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun SettingItem(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun OptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    fullWidth: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val bg = if (selected) colors.primary else colors.surfaceVariant
    val fg = if (selected) colors.onPrimary else colors.onSurfaceVariant
    val mod = if (fullWidth) Modifier.fillMaxWidth() else Modifier.widthIn(min = 80.dp)
    Box(
        modifier = mod
            .heightIn(min = 56.dp)
            .background(bg, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = fg)
    }
}
