package com.zwds.today.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zwds.today.ui.common.BigOutlinedButton
import com.zwds.today.ui.common.BigPrimaryButton
import com.zwds.today.ui.common.ZwdsScaffold

/**
 * OpenAI API Key 申請教學頁
 *
 * 一步一步帶長者完成註冊與付款，避免他們在 OpenAI 官網迷路。
 */
@Composable
fun ApiKeyTutorialScreen(
    onBack: () -> Unit
) {
    val ctx = LocalContext.current

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { ctx.startActivity(intent) }
    }

    ZwdsScaffold(title = "申請 OpenAI API Key", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Header()

            StepCard(
                step = 1,
                title = "前往 OpenAI 官網註冊帳號",
                description = "點選下方按鈕開啟 OpenAI 官網，使用 Email 或 Google 帳號註冊。" +
                    "註冊完成後請依官網指示完成手機驗證。",
                actionText = "前往 OpenAI 註冊頁",
                onAction = { openUrl("https://auth.openai.com/create-account") }
            )

            StepCard(
                step = 2,
                title = "進入 API Platform 並儲值",
                description = "登入後請開啟 OpenAI Platform 後台。系統採「先儲值再使用」，" +
                    "建議首次儲值美金 5–10 元即可使用很久。請至「Settings → Billing」加上信用卡並選擇儲值金額。",
                actionText = "開啟 Billing 設定",
                onAction = { openUrl("https://platform.openai.com/settings/organization/billing/overview") }
            )

            StepCard(
                step = 3,
                title = "建立一支 API Key",
                description = "進入「API keys」頁，點擊右上角「Create new secret key」。" +
                    "Name 可填「今日紫微」、Permissions 選 All、Model 留 Default 即可。" +
                    "建立後系統會顯示一串 sk-... 開頭的字串，請立刻複製（離開頁面後就看不到了）。",
                actionText = "開啟 API keys 頁",
                onAction = { openUrl("https://platform.openai.com/api-keys") }
            )

            StepCard(
                step = 4,
                title = "回到本 App 貼上 Key",
                description = "回到本 App「設定 → AI 設定 → OpenAI API Key」欄位，" +
                    "長按欄位選擇「貼上」，然後按「儲存」即可開始使用。" +
                    "之後每次查詢運勢，本 App 會用您的 API Key 直接呼叫 OpenAI，您只需支付 OpenAI 的少量使用費。"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "為什麼要自備 API Key？",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "1. 您的命盤資料只會傳到您自己的 OpenAI 帳號，不經過第三方 server。\n" +
                            "2. 使用費用由您自己決定上限，不會被任何人代收。\n" +
                            "3. 一次運勢查詢約消耗美金 0.001 元以下，每天問十題也用不到 1 元。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            BigPrimaryButton(text = "我知道了，回到設定頁", onClick = onBack)
            BigOutlinedButton(
                text = "查看 OpenAI 計費說明",
                onClick = { openUrl("https://openai.com/api/pricing/") }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Header() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "四個步驟，您也能擁有自己的 AI 命理師",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
            Text(
                "本 App 使用 OpenAI 的 GPT-5.4-mini 為您撰寫運勢解讀。" +
                    "您需要先到 OpenAI 官網註冊帳號、儲值少量金額並建立一支 API Key，" +
                    "再把 Key 貼回本 App 即可開始使用。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun StepCard(
    step: Int,
    title: String,
    description: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepBadge(step)
                Spacer(Modifier.size(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (actionText != null && onAction != null) {
                BigOutlinedButton(text = actionText, onClick = onAction)
            }
        }
    }
}

@Composable
private fun StepBadge(step: Int) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            step.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}
