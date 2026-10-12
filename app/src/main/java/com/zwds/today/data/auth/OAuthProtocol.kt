package com.zwds.today.data.auth

import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.FormBody
import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

/** Pure JVM OAuth/OIDC validation; never uses unverified identity claims. */
object OAuthProtocol {
    const val ISSUER = "https://auth.openai.com"
    const val RESOURCE = "https://api.openai.com/v1"
    const val PLAN_SCOPE = "chatgpt.tokens.use.direct"
    private val json = Json { ignoreUnknownKeys = true }
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    fun randomValue(): String = encoder.encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) })
    fun challenge(verifier: String): String = encoder.encodeToString(
        MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
    )

    fun authorizationUrl(redirect: String, hostId: String, state: String, nonce: String,
                         verifier: String, clientId: String? = null, idTokenHint: String? = null): String {
        val url = "$ISSUER/api/accounts/authorize".toHttpUrl().newBuilder()
            .addQueryParameter("client_id", clientId ?: "dynamic_agent_client")
            .addQueryParameter("ext_agent_host_id", hostId)
            .addQueryParameter("response_type", "code")
            .addQueryParameter("redirect_uri", redirect)
            .addQueryParameter("scope", "openid profile email offline_access resource.invoke $PLAN_SCOPE")
            .addQueryParameter("resource", RESOURCE)
            .addQueryParameter("state", state).addQueryParameter("nonce", nonce)
            .addQueryParameter("code_challenge_method", "S256")
            .addQueryParameter("code_challenge", challenge(verifier))
        if (clientId == null) url.addQueryParameter("agent_name_hint", "今日紫微")
        if (clientId != null && !idTokenHint.isNullOrBlank()) url.addQueryParameter("id_token_hint", idTokenHint)
        return url.build().toString()
    }

    data class Callback(val code: String, val clientId: String)
    fun codeExchangeForm(callback: Callback, verifier: String, redirect: String): FormBody = FormBody.Builder()
        .add("grant_type", "authorization_code").add("client_id", callback.clientId)
        .add("code", callback.code).add("code_verifier", verifier)
        .add("redirect_uri", redirect).add("resource", RESOURCE).build()

    fun callback(target: String, expectedState: String, savedClientId: String?): Callback {
        require(target.startsWith("/auth/callback?")) { "登入回傳路徑不符" }
        val url = "http://127.0.0.1$target".toHttpUrl()
        require(url.queryParameterValues("state").size == 1 &&
            MessageDigest.isEqual(url.queryParameter("state").orEmpty().toByteArray(), expectedState.toByteArray())) {
            "登入驗證失敗，請重新登入"
        }
        require(url.queryParameter("error") == null) { "登入授權未完成，請重新登入並允許使用 ChatGPT 額度" }
        val returnedId = url.queryParameter("client_id")
        require(url.queryParameterValues("client_id").size <= 1) { "登入註冊回傳不明確" }
        require(savedClientId == null || returnedId == null || returnedId == savedClientId) { "登入帳號的註冊不一致" }
        val id = savedClientId ?: returnedId
        require(!id.isNullOrBlank() && id != "dynamic_agent_client") { "登入註冊未完成" }
        val code = url.queryParameter("code")
        require(!code.isNullOrBlank() && url.queryParameterValues("code").size == 1) { "登入未回傳授權碼" }
        return Callback(code, id)
    }

    data class Identity(val subject: String, val email: String)
    fun verifyIdToken(token: String, jwks: JsonObject, clientId: String, nonce: String?,
                      nowSeconds: Long = System.currentTimeMillis() / 1000): Identity {
        val parts = token.split('.')
        require(parts.size == 3) { "登入憑證格式不正確" }
        fun decode(part: String) = json.parseToJsonElement(
            String(Base64.getUrlDecoder().decode(part), Charsets.UTF_8)
        ).jsonObject
        val header = decode(parts[0])
        require(header["alg"]?.jsonPrimitive?.content == "RS256") { "不支援的登入簽章" }
        val kid = header["kid"]?.jsonPrimitive?.content ?: error("登入簽章缺少識別碼")
        val key = jwks["keys"]!!.jsonArray.map { it.jsonObject }.firstOrNull {
            it["kid"]?.jsonPrimitive?.content == kid && it["kty"]?.jsonPrimitive?.content == "RSA" &&
                (it["use"] == null || it["use"]?.jsonPrimitive?.content == "sig") &&
                (it["alg"] == null || it["alg"]?.jsonPrimitive?.content == "RS256")
        } ?: error("找不到登入驗證公鑰")
        fun integer(name: String) = BigInteger(1, Base64.getUrlDecoder().decode(key[name]!!.jsonPrimitive.content))
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(integer("n"), integer("e")))
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initVerify(publicKey)
        signature.update("${parts[0]}.${parts[1]}".toByteArray(Charsets.US_ASCII))
        require(signature.verify(Base64.getUrlDecoder().decode(parts[2]))) { "登入憑證簽章驗證失敗" }
        val claims = decode(parts[1])
        require(claims["iss"]?.jsonPrimitive?.content == ISSUER) { "登入憑證來源不符" }
        val aud = claims["aud"]
        require(if (aud is JsonArray) aud.any { it.jsonPrimitive.content == clientId }
                else aud?.jsonPrimitive?.content == clientId) { "登入憑證不屬於此 APP" }
        if (aud is JsonArray && aud.size > 1) require(claims["azp"]?.jsonPrimitive?.content == clientId)
        require((claims["exp"]?.jsonPrimitive?.longOrNull ?: 0) > nowSeconds) { "登入憑證已過期" }
        require((claims["iat"]?.jsonPrimitive?.longOrNull ?: Long.MAX_VALUE) <= nowSeconds + 60) { "登入憑證時間不符" }
        claims["nbf"]?.jsonPrimitive?.longOrNull?.let { require(it <= nowSeconds + 60) }
        if (nonce != null) require(claims["nonce"]?.jsonPrimitive?.content == nonce) { "登入驗證碼不符" }
        val sub = claims["sub"]?.jsonPrimitive?.content.orEmpty()
        require(sub.isNotBlank()) { "登入憑證缺少帳號識別" }
        return Identity(sub, claims["email"]?.jsonPrimitive?.content.orEmpty())
    }
}
