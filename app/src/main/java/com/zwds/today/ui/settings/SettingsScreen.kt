package com.zwds.today.ui.settings

import android.Manifest
import android.os.Build
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zwds.today.BuildConfig
import com.zwds.today.data.prefs.ThemeMode
import com.zwds.today.data.update.ReleaseInfo
import com.zwds.today.di.ServiceLocator
import com.zwds.today.di.ZwdsViewModelFactory
import com.zwds.today.domain.model.ZiHourSchool
import com.zwds.today.ui.common.*
import com.zwds.today.ui.theme.FontScale
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onShowDisclaimer: () -> Unit, onOpenAccount: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = ZwdsViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()
    val account by vm.account.collectAsStateWithLifecycle()
    val updates by ServiceLocator.updateRepository.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var notificationGranted by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationGranted = it
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME)
                notificationGranted = NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ZwdsScaffold(title = "設定", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SettingsCard("我的帳號", "使用自己的帳號，開始每天的個人化解讀。") {
                Text(if (account.serviceReady) "解讀服務已就緒" else if (account.connected) "已登入 · 請查看會員權限" else "尚未登入",
                    style = MaterialTheme.typography.titleMedium)
                BigPrimaryButton(if (account.connected) "查看帳號" else "前往登入", onOpenAccount)
            }
            SettingsCard("文字與外觀", "保留大字與清楚的觸控區域。") {
                Text("字體大小", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FontScale.entries.forEach { scale -> OptionChip(scale.displayName, state.fontScale == scale,
                        { vm.setFontScale(scale) }) }
                }
                Text("色彩模式", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode -> OptionChip(mode.cnName, state.themeMode == mode,
                        { vm.setThemeMode(mode) }) }
                }
            }
            SettingsCard("版本更新", "目前版本 ${BuildConfig.VERSION_NAME}") {
                ToggleRow("自動檢查新版", "有新版本時，在 APP 內提醒您更新。",
                    updates.enabled, ServiceLocator.updateRepository::setEnabled)
                if (updates.enabled) {
                    BigOutlinedButton("開啟系統更新通知", {
                        if (Build.VERSION.SDK_INT >= 33 && !notificationGranted) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    })
                    Text(if (notificationGranted) "系統通知已允許" else "未允許系統通知時，仍會在 APP 內提示新版。",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BigOutlinedButton(if (updates.checking) "檢查中…" else "立即檢查更新", {
                    scope.launch { ServiceLocator.updateRepository.check(manual = true) }
                }, enabled = !updates.checking)
                updates.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                BigOutlinedButton("查看版本與下載", { openWebPage(context, ReleaseInfo.RELEASES_URL) })
            }
            SettingsCard("命盤選項", "依您的閱讀習慣調整。") {
                ToggleRow("顯示完整命盤", "在首頁提供完整命盤入口", state.showFullChart, vm::setShowFullChart)
                Text("23 點出生（晚子時）歸屬", style = MaterialTheme.typography.titleMedium)
                OptionChip("晚子時歸明日", state.ziHourSchool == ZiHourSchool.LATE_BELONGS_TO_NEXT_DAY,
                    { vm.setZiHourSchool(ZiHourSchool.LATE_BELONGS_TO_NEXT_DAY) }, true)
                OptionChip("晚子時歸今日", state.ziHourSchool == ZiHourSchool.LATE_BELONGS_TO_SAME_DAY,
                    { vm.setZiHourSchool(ZiHourSchool.LATE_BELONGS_TO_SAME_DAY) }, true)
            }
            BigOutlinedButton("查看免責聲明", onShowDisclaimer)
            Text("今日紫微 · ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
        }
    }

}

@Composable
private fun SettingsCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(title, subtitle)
        EditorialPanel {
            content()
        }
    }
}
@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onCheckedChange = onChange)
    }
}
@Composable
private fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit, fullWidth: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val bg = if (selected) colors.primaryContainer else colors.surfaceVariant
    val fg = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = bg,
        modifier = (if (fullWidth) Modifier.fillMaxWidth() else Modifier.widthIn(min = 80.dp)).heightIn(min = 48.dp)) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = fg)
        }
    }
}
