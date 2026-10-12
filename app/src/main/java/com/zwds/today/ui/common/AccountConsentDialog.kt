package com.zwds.today.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/** Keep the daily interface simple while explaining who receives the authorization and chart summary. */
@Composable
fun AccountConsentDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val textHeight = (LocalConfiguration.current.screenHeightDp / 2).dp
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("會員使用授權") },
        text = { Text("將前往 OpenAI 官方頁面，登入並授權今日紫微使用您的 ChatGPT 會員額度。此功能需要符合資格的 Plus／Pro 帳號，實際可用性依 OpenAI 授權結果為準。\n\n新解讀會使用會員額度，並將命盤星曜摘要、查詢日期與主題傳送至 OpenAI。姓名、完整出生資料與歷史紀錄留在本機。\n\n可以隨時登出，或至 ChatGPT 設定調整使用上限與撤銷授權。",
            modifier = Modifier.heightIn(max = textHeight).verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("繼續登入") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
