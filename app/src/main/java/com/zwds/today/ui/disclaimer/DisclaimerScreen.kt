package com.zwds.today.ui.disclaimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zwds.today.ui.common.BigOutlinedButton
import com.zwds.today.ui.common.BigPrimaryButton
import com.zwds.today.ui.common.ZwdsScaffold

@Composable
fun DisclaimerScreen(
    isFirstLaunch: Boolean = true,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    ZwdsScaffold(
        title = if (isFirstLaunch) "歡迎使用今日紫微" else "免責聲明"
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            com.zwds.today.ui.common.StarHero("開始之前", "星曜作為提醒，\n日子由您決定。", "留一點時間認識今日紫微。")

            Section("這是什麼樣的 App") {
                Text(
                    "今日紫微結合傳統紫微斗數與個人化文字解讀，" +
                        "為您與家人撰寫每日的個人化運勢解讀。" +
                        "命盤計算在本機進行，文字解讀由自動化服務生成。",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Section("它不能取代什麼") {
                Text(
                    "・本 App 提供的所有建議都是「生活提醒」，不是醫療建議、不是投資建議、" +
                        "不是法律建議。\n" +
                        "・身體不舒服請看醫生、財務問題請問專業人士、法律問題請找律師。\n" +
                        "・命理只是一種文化參考，請以平常心看待，不要影響日常的判斷。",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Section("關於您的個人資料") {
                Text(
                    "・家人檔案與查詢記錄保存在您的手機，不會上傳給開發者。\n" +
                    "・產生新解讀時，命盤星曜摘要、查詢日期與主題會傳送至 OpenAI。\n" +
                    "・姓名、完整出生資料與歷史記錄保存在此裝置。\n" +
                    "・送出的摘要資料依 OpenAI 的政策處理。",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Section("帳號與解讀") {
                Text(
                    "・到「設定 → 我的帳號」連接自己的帳號，無需手動設定。\n" +
                    "・新解讀使用您授權的 ChatGPT 會員額度，需要符合資格的 Plus／Pro 帳號，可用性依實際授權結果為準。\n" +
                    "・可在設定登出此裝置，或到帳號管理頁撤銷授權。",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (isFirstLaunch)
                    "點選下方「我同意，開始使用」即代表您已閱讀並理解上述內容。"
                else
                    "若您不同意，可選擇不繼續使用本 App。",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            BigPrimaryButton(
                text = if (isFirstLaunch) "我同意，開始使用" else "我已閱讀完畢",
                onClick = onAccept
            )

            if (isFirstLaunch) {
                BigOutlinedButton(
                    text = "暫不使用",
                    onClick = onDecline
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}
