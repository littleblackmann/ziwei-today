package com.zwds.today.ui.settings

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zwds.today.BuildConfig
import com.zwds.today.R
import com.zwds.today.data.auth.LoginStep
import com.zwds.today.di.ZwdsViewModelFactory
import com.zwds.today.ui.common.*

@Composable
fun AccountScreen(onBack: () -> Unit, onReady: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = ZwdsViewModelFactory)
    val account by vm.account.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var consent by remember { mutableStateOf(false) }
    var different by remember { mutableStateOf(false) }
    var logout by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (account.connected) vm.refreshStatus() }

    ZwdsScaffold("我的帳號", onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            StarHero("專屬於您的每日解讀", "自己的帳號，\n每日的提醒。", "登入一次，讓每天的提醒更貼近您。")
            EditorialPanel {
                if (account.signingIn) {
                    SectionTitle("正在連接帳號")
                    listOf("在瀏覽器登入與授權", "驗證帳號與會員權限", "確認解讀服務").forEachIndexed { index, label ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val done = index < account.loginStep.ordinal
                            Icon(if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null,
                                tint = if (index <= account.loginStep.ordinal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("完成瀏覽器授權後，請返回此頁查看結果。", style = MaterialTheme.typography.bodySmall)
                    TextButton(vm::cancelSignIn) { Text("取消這次登入") }
                } else {
                    SectionTitle(if (account.connected) "已連接的帳號" else "連接 ChatGPT 會員",
                        if (account.connected) account.email.ifBlank { "您的 ChatGPT 帳號" }
                        else "使用符合資格的 Plus／Pro 會員，無需手動設定。")
                    if (account.connected) {
                        SuggestionChip(onClick = {}, label = { Text(when {
                            account.busy -> "正在確認服務"
                            account.serviceReady -> "已取得解讀權限"
                            account.planEnabled -> "已授權會員額度 · 服務待確認"
                            else -> "會員額度尚未授權"
                        }) }, icon = { Icon(Icons.Default.VerifiedUser, null, Modifier.size(18.dp)) })
                        if (account.serviceReady) BigPrimaryButton("開始今日解讀", onReady, enabled = !account.busy)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(vm::refreshStatus, enabled = !account.busy) { Text("重新確認連線") }
                            TextButton({ different = true; consent = true }, enabled = !account.busy) { Text("切換帳號") }
                        }
                    }
                    if (!account.connected || !account.serviceReady) {
                        Button(onClick = { different = false; consent = true }, enabled = !account.busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)) {
                            Icon(painterResource(R.drawable.ic_chatgpt), null, Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Continue with ChatGPT", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    if (account.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                account.message?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                account.diagnostic?.let { diagnostic ->
                    HorizontalDivider()
                    Text("連線資訊", style = MaterialTheme.typography.labelMedium)
                    Text(diagnostic, style = MaterialTheme.typography.bodySmall)
                    TextButton({
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                            .setPrimaryClip(ClipData.newPlainText("登入連線資訊", "今日紫微 ${BuildConfig.VERSION_NAME}\n$diagnostic"))
                        copied = true
                    }) { Text(if (copied) "已複製" else "複製連線資訊") }
                }
            }
            EditorialPanel {
                SectionTitle("關於會員使用")
                Text("新解讀會使用您授權的 ChatGPT 會員額度，與其他使用該額度的功能共用限制。可用性依帳號資格與 OpenAI 授權結果而定。",
                    style = MaterialTheme.typography.bodyMedium)
                Text("家人檔案與歷史紀錄保存在此裝置。產生新解讀時，只傳送命盤星曜摘要、查詢日期與主題至 OpenAI。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton({ openWebPage(context, "https://chatgpt.com/#settings") }) { Text("到 ChatGPT 管理額度與授權") }
            }
            if (account.connected) TextButton({ logout = true }, enabled = !account.busy,
                modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("登出此裝置") }
            Spacer(Modifier.height(12.dp))
        }
    }
    if (consent) AccountConsentDialog({ consent = false }, { consent = false; vm.signIn(different) })
    if (logout) AlertDialog(onDismissRequest = { logout = false }, title = { Text("登出此裝置？") },
        text = { Text("家人檔案與解讀紀錄會保留。重新登入後就能再次產生新解讀。") },
        confirmButton = { TextButton({ logout = false; vm.signOut() }) { Text("登出") } },
        dismissButton = { TextButton({ logout = false }) { Text("取消") } })
    if (account.showPlanWelcome && !account.signingIn) AlertDialog(
        onDismissRequest = vm::acknowledgePlanWelcome, title = { Text("會員額度已授權") },
        text = { Text("產生新解讀會使用您授權的 ChatGPT 會員額度。可以隨時到 ChatGPT 管理今日紫微的使用上限或撤銷授權。") },
        confirmButton = { TextButton(vm::acknowledgePlanWelcome) { Text("知道了") } })
}
