package com.zwds.today.data.auth

import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.util.Base64

class OAuthProtocolTest {
    @Test fun exchangeUsesIssuedClientExactRedirectAndMatchingPkce() {
        val redirect = "http://127.0.0.1:41021/auth/callback"
        val verifier = OAuthProtocol.randomValue()
        val authorize = OAuthProtocol.authorizationUrl(redirect, "urn:uuid:test", "state", "nonce", verifier).toHttpUrl()
        val callback = OAuthProtocol.callback("/auth/callback?state=state&client_id=oaiapp_issued&code=code%2Bwith%2Freserved%25", "state", null)
        val form = OAuthProtocol.codeExchangeForm(callback, verifier, redirect)
        val fields = (0 until form.size).associate { form.name(it) to form.value(it) }
        assertEquals("oaiapp_issued", fields["client_id"])
        assertEquals(authorize.queryParameter("redirect_uri"), fields["redirect_uri"])
        assertEquals(authorize.queryParameter("resource"), fields["resource"])
        assertEquals(authorize.queryParameter("code_challenge"), OAuthProtocol.challenge(fields["code_verifier"]!!))
        assertEquals("code+with/reserved%", fields["code"])
        assertFalse(fields.containsKey("client_secret"))
        assertEquals("authorization_code", fields["grant_type"])
    }
    @Test fun pkceMatchesRfc7636Vector() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            OAuthProtocol.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
    }
    @Test fun firstAndReturningRegistrationKeepCorrectClientAndHost() {
        val initial = OAuthProtocol.authorizationUrl("http://127.0.0.1:54321/auth/callback", "urn:uuid:host",
            "state", "nonce", "verifier").toHttpUrl()
        assertEquals("dynamic_agent_client", initial.queryParameter("client_id"))
        assertEquals("今日紫微", initial.queryParameter("agent_name_hint"))
        assertEquals("S256", initial.queryParameter("code_challenge_method"))
        assertTrue(initial.queryParameter("scope")!!.contains(OAuthProtocol.PLAN_SCOPE))
        val returning = OAuthProtocol.authorizationUrl("http://127.0.0.1:54322/auth/callback", "urn:uuid:host",
            "state2", "nonce2", "verifier2", "oaiapp_saved", "verified-id-hint").toHttpUrl()
        assertEquals("oaiapp_saved", returning.queryParameter("client_id"))
        assertNull(returning.queryParameter("agent_name_hint"))
        assertEquals("verified-id-hint", returning.queryParameter("id_token_hint"))
        assertEquals(initial.queryParameter("ext_agent_host_id"), returning.queryParameter("ext_agent_host_id"))
    }
    @Test fun callbackRejectsCsrfDenialMissingOrSwitchedClient() {
        assertEquals("oaiapp_one", OAuthProtocol.callback("/auth/callback?state=s&code=c&client_id=oaiapp_one", "s", null).clientId)
        val invalid = listOf("/other?state=s&code=c&client_id=id", "/auth/callback?state=x&code=c&client_id=id",
            "/auth/callback?state=s&error=access_denied", "/auth/callback?state=s&code=c",
            "/auth/callback?state=s&state=s&code=c&client_id=id",
            "/auth/callback?state=s&code=c&client_id=dynamic_agent_client")
        invalid.forEach { assertThrows(IllegalArgumentException::class.java) { OAuthProtocol.callback(it, "s", null) } }
        assertThrows(IllegalArgumentException::class.java) {
            OAuthProtocol.callback("/auth/callback?state=s&code=c&client_id=other", "s", "saved")
        }
    }
    @Test fun verifiesSignatureIssuerAudienceExpiryAndNonce() {
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val publicKey = pair.public as RSAPublicKey
        val encoder = Base64.getUrlEncoder().withoutPadding()
        fun unsigned(bytes: ByteArray) = encoder.encodeToString(if (bytes[0] == 0.toByte()) bytes.drop(1).toByteArray() else bytes)
        val jwks = buildJsonObject { put("keys", buildJsonArray { add(buildJsonObject {
            put("kty", "RSA"); put("kid", "test"); put("n", unsigned(publicKey.modulus.toByteArray()))
            put("e", unsigned(publicKey.publicExponent.toByteArray()))
        }) }) }
        val claims = buildJsonObject { put("iss", OAuthProtocol.ISSUER); put("aud", "client")
            put("exp", 2000); put("iat", 500); put("nonce", "nonce"); put("sub", "subject"); put("email", "test@example.com") }
        fun token(payload: JsonObject, key: java.security.PrivateKey = pair.private): String {
            val header = encoder.encodeToString("{\"alg\":\"RS256\",\"kid\":\"test\"}".toByteArray())
            val encoded = "$header.${encoder.encodeToString(payload.toString().toByteArray())}"
            val signature = Signature.getInstance("SHA256withRSA").apply { initSign(key); update(encoded.toByteArray()) }.sign()
            return "$encoded.${encoder.encodeToString(signature)}"
        }
        val good = token(claims)
        assertEquals("subject", OAuthProtocol.verifyIdToken(good, jwks, "client", "nonce", 1000).subject)
        listOf("iss" to JsonPrimitive("https://evil.example"), "aud" to JsonPrimitive("other"),
            "nonce" to JsonPrimitive("wrong"), "exp" to JsonPrimitive(999)).forEach { (key, value) ->
            assertThrows(IllegalArgumentException::class.java) {
                OAuthProtocol.verifyIdToken(token(JsonObject(claims.toMutableMap().apply { put(key, value) })), jwks, "client", "nonce", 1000)
            }
        }
        val other = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        assertThrows(IllegalArgumentException::class.java) { OAuthProtocol.verifyIdToken(token(claims, other.private), jwks, "client", "nonce", 1000) }
    }
}
