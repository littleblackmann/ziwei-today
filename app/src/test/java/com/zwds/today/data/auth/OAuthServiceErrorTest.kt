package com.zwds.today.data.auth

import org.junit.Assert.*
import org.junit.Test

class OAuthServiceErrorTest {
    @Test fun membershipQuotaErrorsAreExplainedWithoutExposingProviderText() {
        val error = OAuthServiceError.parse(OAuthStage.INFERENCE, 403,
            """{"error":{"code":"subscription_sharing_usage_limit_exceeded","message":"private@example.com"}}""", null)
        assertTrue(error.message.contains("會員使用限制"))
        assertFalse(error.message.contains("private"))
    }
    @Test fun oauthBadGrantIsRetryableCodeExchangeFailure() {
        val error = OAuthServiceError.parse(OAuthStage.CODE_EXCHANGE, 400,
            """{"error":"invalid_grant","error_description":"ac_private_code refresh_token=private"}""", "req_test123")
        assertEquals("invalid_grant", error.code)
        assertTrue(error.message.contains("授權碼"))
        assertTrue(error.diagnostic.contains("登入憑證交換"))
        assertTrue(error.diagnostic.contains("HTTP 400"))
        assertTrue(error.diagnostic.contains("req_test123"))
        assertFalse(error.message.contains("private"))
        assertFalse(error.message.contains("refresh_token="))
    }
    @Test fun structuredProviderErrorKeepsItsCodeAndStage() {
        val error = OAuthServiceError.parse(OAuthStage.MODELS, 403,
            """{"error":{"code":"subscription_sharing_user_not_eligible","message":"private@example.com"}}""", null)
        assertEquals("subscription_sharing_user_not_eligible", error.code)
        assertTrue(error.message.contains("資格"))
        assertTrue(error.diagnostic.contains("確認解讀服務"))
        assertFalse(error.message.contains("@"))
    }
    @Test fun malformedOrUnexpectedResponsesNeverExposeRawBody() {
        listOf("<html>private token ac_secret</html>", """{"detail":"private jwt token"}""",
            """{"error":"ac_secret"}""", "null", "[]", """{"error":{"code":"sk-secret"}}""").forEach { body ->
            val error = OAuthServiceError.parse(OAuthStage.CODE_EXCHANGE, 400, body, "ac_secret")
            assertNull(error.code)
            assertNull(error.requestId)
            assertFalse(error.message.contains("secret"))
            assertFalse(error.message.contains("private"))
        }
    }
    @Test fun identityVerificationAndRefreshDoNotMasqueradeAsCodeExchange() {
        val keys = OAuthServiceError.parse(OAuthStage.IDENTITY_KEYS, 400, "{}", null)
        val refresh = OAuthServiceError.parse(OAuthStage.REFRESH, 400, """{"error":"invalid_grant"}""", null)
        assertTrue(keys.diagnostic.contains("簽章驗證"))
        assertTrue(refresh.message.contains("登入授權已失效"))
        assertFalse(refresh.message.contains("授權碼已失效"))
    }
}
