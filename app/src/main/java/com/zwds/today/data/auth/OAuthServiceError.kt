package com.zwds.today.data.auth

import kotlinx.serialization.json.*

enum class OAuthStage(val label: String) {
    CODE_EXCHANGE("登入憑證交換"), REFRESH("登入憑證更新"), IDENTITY_KEYS("登入簽章驗證"), MODELS("確認解讀服務"), INFERENCE("產生解讀")
}

/** Provider error text can contain credentials. Only known codes and request IDs leave this parser. */
class OAuthServiceError(val stage: OAuthStage, val status: Int, val code: String?,
                        val bodyShape: String, val requestId: String?) : Exception() {
    val diagnostic: String get() = listOfNotNull(stage.label, "HTTP $status", code,
        "格式 $bodyShape", requestId?.let { "request_id=$it" }).joinToString(" · ")

    override val message: String get() {
        val explanation = when (code) {
            "invalid_grant" -> if (stage == OAuthStage.REFRESH) "登入授權已失效，請重新登入。"
                else "授權碼已失效或無法驗證。請再次按登入，使用新的授權碼重試。"
            "invalid_client", "unauthorized_client" -> "登入註冊未被接受，請重新建立登入連線。"
            "invalid_scope", "invalid_target", "invalid_resource", "invalid_request" ->
                "登入服務拒絕此授權請求，請提供下方錯誤資訊以確認原因。"
            "subscription_sharing_user_not_eligible" -> "此帳號尚不符合 APP 使用 ChatGPT 方案額度的資格。"
            "subscription_sharing_usage_limit_exceeded", "rate_limit_exceeded" ->
                "已達 ChatGPT 會員使用限制，請至 ChatGPT 查看額度或稍後再試。"
            "subscription_sharing_usage_unavailable" ->
                "目前無法使用此帳號的會員額度，請查看 ChatGPT 的方案與 APP 授權設定。"
            "access_denied" -> "此帳號未允許這次授權，請重新登入確認。"
            else -> when (status) {
                401, 403 -> "登入或帳號權限未被接受，請確認授權與帳號方案。"
                429 -> "目前已達使用限制，請稍後再試。"
                in 500..599 -> "登入服務暫時無法使用，請稍後再試。"
                else -> "${stage.label}未完成，請提供下方錯誤資訊以確認原因。"
            }
        }
        return explanation
    }

    companion object {
        private val knownCodes = setOf("invalid_request", "invalid_grant", "invalid_client", "unauthorized_client",
            "unsupported_grant_type", "invalid_scope", "invalid_target", "invalid_resource", "access_denied",
            "temporarily_unavailable", "server_error", "subscription_sharing_user_not_eligible",
            "invalid_refresh_token", "token_expired", "refresh_token_expired", "refresh_token_invalidated",
            "refresh_token_reused", "subscription_sharing_usage_limit_exceeded", "rate_limit_exceeded",
            "subscription_sharing_usage_unavailable")

        fun parse(stage: OAuthStage, status: Int, body: String, requestId: String?): OAuthServiceError {
            val root = runCatching { Json.parseToJsonElement(body) }.getOrNull()
            val error = (root as? JsonObject)?.get("error")
            val candidate = when (error) {
                is JsonPrimitive -> error.contentOrNull
                is JsonObject -> (error["code"] as? JsonPrimitive)?.contentOrNull
                else -> null
            }
            val shape = when {
                root !is JsonObject -> "非 JSON 物件"
                error is JsonObject -> "error 物件"
                error is JsonPrimitive -> "OAuth error"
                "detail" in root -> "detail 物件"
                else -> "JSON 物件"
            }
            val safeId = requestId?.takeIf {
                it.matches(Regex("req_[A-Za-z0-9_-]{1,100}|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
            }
            return OAuthServiceError(stage, status, candidate?.takeIf { it in knownCodes }, shape, safeId)
        }
    }
}
