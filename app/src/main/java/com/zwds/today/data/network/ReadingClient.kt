package com.zwds.today.data.network

import com.zwds.today.data.auth.*
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ReadingClient(private val auth: ChatGptAuthRepository) {
    private val http = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS).callTimeout(150, TimeUnit.SECONDS).build()
    suspend fun generateTopicFortune(systemInstructions: String, userPrompt: String): GeneratedFortune = withContext(Dispatchers.IO) {
        val receipt = auth.receipt()
        val session = auth.authorizedSession()
        if (session.selectedModel.isBlank()) throw ChatGptSignInRequired("此帳號目前沒有可用的解讀服務，請查看帳號狀態")
        val request = Request.Builder().url("${OAuthProtocol.RESOURCE}/responses")
            .header("Authorization", "Bearer ${session.accessToken}")
            .header("Accept", "text/event-stream")
            .post(ReadingPayload.buildRequestBody(session.selectedModel, systemInstructions, userPrompt)
                .toString().toRequestBody("application/json".toMediaType())).build()
        try {
            coroutineScope {
                val call = http.newCall(request)
                val cancellation = launch(start = CoroutineStart.UNDISPATCHED) {
                    try { awaitCancellation() } finally { call.cancel() }
                }
                try {
                    call.execute().use { response ->
                        if (!response.isSuccessful) {
                            val error = OAuthServiceError.parse(OAuthStage.INFERENCE, response.code,
                                response.body?.string().orEmpty(), response.header("x-request-id"))
                            if (response.code == 401) {
                                auth.invalidate(receipt)
                                throw ChatGptSignInRequired("登入已失效，請重新登入")
                            }
                            throw OpenAiException(error.message, usageLimit = response.code == 429 ||
                                error.code in setOf("subscription_sharing_usage_limit_exceeded", "rate_limit_exceeded"), diagnostic = error.diagnostic)
                        }
                        val safeRequestId = OAuthServiceError.parse(OAuthStage.INFERENCE, response.code,
                            "{}", response.header("x-request-id")).requestId
                        val connectionInfo = listOfNotNull("產生解讀", "HTTP ${response.code}",
                            safeRequestId?.let { "request_id=$it" }).joinToString(" · ")
                        val body = response.body ?: throw OpenAiException("尚未收到解讀，請重試", diagnostic = connectionInfo)
                        val result = try {
                            ReadingPayload.parseFortune(ResponsesStream.read(body.charStream().buffered(), response.header("x-request-id")))
                        } catch (e: OpenAiException) {
                            throw OpenAiException(e.message ?: "無法取得解讀", usageLimit = e.usageLimit,
                                diagnostic = e.diagnostic?.let { "HTTP ${response.code} · $it" }
                                    ?: "$connectionInfo · 解讀格式驗證未通過")
                        }
                        currentCoroutineContext().ensureActive()
                        auth.withCurrentSession(receipt) { GeneratedFortune(result, receipt) }
                    }
                } finally { cancellation.cancel() }
            }
        } catch (e: IOException) {
            currentCoroutineContext().ensureActive()
            throw OpenAiException("網路連線中斷或等待逾時，請重試", e, diagnostic = "產生解讀 · network_interrupted")
        }
    }
    suspend fun <T> saveIfCurrent(result: GeneratedFortune, save: suspend () -> T): T =
        auth.withCurrentSession(result.receipt) { currentCoroutineContext().ensureActive(); save() }
}

data class GeneratedFortune(val fortune: TopicFortuneJson, internal val receipt: Long)
data class TopicFortuneJson(val title: String, val level: String, val advice: String,
                            val tags: List<String>, val focusSummary: String)
