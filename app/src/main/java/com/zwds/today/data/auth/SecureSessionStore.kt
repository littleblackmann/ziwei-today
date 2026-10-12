package com.zwds.today.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyStore
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Serializable
data class ChatGptSession(
    val clientId: String, val subject: String, val email: String,
    val accessToken: String, val refreshToken: String, val idToken: String,
    val scopes: List<String>, val expiresAt: Long, val selectedModel: String = ""
) {
    override fun toString() = "ChatGptSession(credentials=redacted)"
}

/** A single atomic encrypted record, with the AES key kept in Android Keystore. */
class SecureSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("chatgpt_session", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    val hostId: String = prefs.getString("host_id", null) ?: "urn:uuid:${UUID.randomUUID()}".also {
        check(prefs.edit().putString("host_id", it).commit())
    }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("zwds_chatgpt_v1", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("zwds_chatgpt_v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    @Synchronized fun read(): ChatGptSession? {
        val record = prefs.getString("encrypted_session", null) ?: return null
        return runCatching {
            val parts = record.split('.')
            val decoder = Base64.getDecoder()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, decoder.decode(parts[0])))
            json.decodeFromString<ChatGptSession>(String(cipher.doFinal(decoder.decode(parts[1])), Charsets.UTF_8))
        }.getOrElse { clear(); null }
    }
    @Synchronized fun save(session: ChatGptSession) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(json.encodeToString(session).toByteArray(Charsets.UTF_8))
        val encoder = Base64.getEncoder()
        check(prefs.edit().putString("encrypted_session",
            "${encoder.encodeToString(cipher.iv)}.${encoder.encodeToString(encrypted)}")
            .putString("last_client_id", session.clientId).putString("last_subject", session.subject)
            .remove("pending_client_id").commit())
    }
    @Synchronized fun clear() { check(prefs.edit().remove("encrypted_session").commit()) }
    val planWelcomeAcknowledged: Boolean get() = prefs.getBoolean("plan_welcome_acknowledged", false)
    fun acknowledgePlanWelcome() { prefs.edit().putBoolean("plan_welcome_acknowledged", true).apply() }
    // A state-validated callback may issue a client before the code exchange fails.
    // This ID can retry registration, but is never treated as a verified identity or session.
    fun rememberPendingClient(id: String) { check(prefs.edit().putString("pending_client_id", id).commit()) }
    fun clearPendingClient() { check(prefs.edit().remove("pending_client_id").commit()) }
    fun clearRegistration() { check(prefs.edit().remove("last_client_id").remove("last_subject").remove("pending_client_id").commit()) }
    fun lastRegistration(): Pair<String, String?>? {
        val id = prefs.getString("last_client_id", null)
        if (id != null) return id to prefs.getString("last_subject", null)
        return prefs.getString("pending_client_id", null)?.let { it to null }
    }
}
