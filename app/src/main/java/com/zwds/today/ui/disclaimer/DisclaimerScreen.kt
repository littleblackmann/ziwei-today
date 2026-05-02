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
            Text(
                text = "請先閱讀使用說明",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Section("這是什麼樣的 App") {
                Text(
                    "今日紫微結合中國傳統紫微斗數與 OpenAI GPT-5.4-mini，" +
                        "為您與家人撰寫每日的個人化運勢解讀。" +
                        "命盤計算在本機進行，文字解讀則由 AI 即時生成。",
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

            Section("關於您的個人資料與 OpenAI") {
                Text(
                    "・家人的姓名、出生資料只儲存在您的手機，本 App 不會把它們上傳給開發者或任何第三方 server。\n" +
                        "・每次按「開始今日詢問」並產生運勢解讀時，App 會把該位家人的姓名、" +
                        "出生資料、命盤摘要直接傳送到您自己的 OpenAI 帳號（透過您輸入的 API Key），" +
                        "由 OpenAI 撰寫文字後回傳。\n" +
                        "・依 OpenAI 政策，API 請求預設不會被用於模型訓練；詳細條款請見 OpenAI 官網。\n" +
                        "・若您不希望任何資料離開手機，請不要使用本 App 的查詢功能。",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Section("您需要自備 OpenAI API Key") {
                Text(
                    "・本 App 沒有內建任何 API Key，每位使用者需自行註冊 OpenAI 帳號並建立自己的 API Key。\n" +
                        "・OpenAI 採量計費（每次查詢約美金 0.001 元以下），費用由您直接付給 OpenAI。\n" +
                        "・進入「設定 → AI 設定」可看到完整申請教學。",
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
