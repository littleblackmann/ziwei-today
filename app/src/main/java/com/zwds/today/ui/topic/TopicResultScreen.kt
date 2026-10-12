package com.zwds.today.ui.topic

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zwds.today.BuildConfig
import com.zwds.today.di.ZwdsViewModelFactory
import com.zwds.today.domain.model.*
import com.zwds.today.ui.common.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopicResultScreen(memberId: Long, topic: FortuneTopic, date: java.time.LocalDate = java.time.LocalDate.now(),
    onBack: () -> Unit, onOpenAccountSettings: () -> Unit) {
    val vm: TopicResultViewModel = viewModel(factory = ZwdsViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(memberId, topic, date) { vm.load(memberId, topic, date = date) }
    ZwdsScaffold(topic.cnName, onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            when {
                state.loading -> EditorialPanel {
                    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        CircularProgressIndicator()
                        Text("正在整理今天的星曜與提醒…", style = MaterialTheme.typography.titleMedium)
                        Text("給自己一點慢下來的時間。", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                state.error != null -> {
                    TopicResultErrorPanel(state.error!!, vm::retry, onOpenAccountSettings)
                }
                state.fortune != null -> {
                    val fortune = state.fortune!!
                    Text("${fortune.familyMemberName} · ${state.date}" + if (state.fromCache) " · 已保存" else "",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    StarHero(fortune.level.cnName + " · " + fortune.topic.cnName, fortune.title, fortune.focusSummary)
                    SectionTitle("留意這幾件事")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        fortune.tags.forEach { tag ->
                            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                                Text(tag, Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                    EditorialPanel {
                        SectionTitle("給今天的您")
                        fortune.advice.replace("\r\n", "\n").split(Regex("\n+")).filter { it.isNotBlank() }.forEach {
                            Text(it.trim(), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Text("這是一份生活與文化參考，重要決定仍請依實際情況判斷。",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    BigOutlinedButton("重新解讀", vm::regenerate, leadingIcon = Icons.Default.Refresh)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
internal fun TopicResultErrorPanel(error: TopicResultError, onRetry: () -> Unit, onOpenAccount: () -> Unit) {
    val context = LocalContext.current
    var expanded by remember(error) { mutableStateOf(false) }
    var copied by remember(error) { mutableStateOf(false) }
    EditorialPanel {
        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
        SectionTitle(if (error is TopicResultError.SignInRequired) "連接帳號，開始解讀" else "這份解讀暫時還沒準備好",
            when (error) {
                TopicResultError.SignInRequired -> "登入並授權自己的會員後，就可以產生新的解讀。"
                is TopicResultError.UsageLimit -> error.message
                is TopicResultError.Network -> error.message
                is TopicResultError.Unknown -> error.message
            })
        if (error !is TopicResultError.SignInRequired) BigPrimaryButton("再試一次", onRetry)
        BigOutlinedButton("查看我的帳號", onOpenAccount)
        error.diagnostic?.let { diagnostic ->
            TextButton({ expanded = !expanded }) { Text(if (expanded) "收合錯誤資訊" else "檢視錯誤資訊") }
            if (expanded) {
                HorizontalDivider()
                Text(diagnostic, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton({
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                        ClipData.newPlainText("解讀錯誤資訊", "今日紫微 ${BuildConfig.VERSION_NAME}\n$diagnostic"))
                    copied = true
                }) { Text(if (copied) "已複製" else "複製錯誤資訊") }
            }
        }
    }
}
