package com.zwds.today.data.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import okhttp3.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

data class ChatGptModel(val id: String, val name: String)
enum class LoginStep { BROWSER, VERIFY, SERVICE }
data class ChatGptAccountState(
    val connected: Boolean = false, val email: String = "", val planEnabled: Boolean = false,
    val busy: Boolean = false, val models: List<ChatGptModel> = emptyList(),
    val selectedModel: String = "", val message: String? = null, val showPlanWelcome: Boolean = false,
    val diagnostic: String? = null, val signingIn: Boolean = false,
    val loginStep: LoginStep = LoginStep.BROWSER, val serviceReady: Boolean = false
)
class ChatGptSignInRequired(message: String = "請先登入 ChatGPT 帳號") : Exception(message)

class ChatGptAuthRepository(private val context: Context) {
    private val store = SecureSessionStore(context)
    private val http = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var session = store.read()
    private val _state = MutableStateFlow(summary())
    val state = _state.asStateFlow()
    @Volatile private var callbackServer: ServerSocket? = null
    private val generation = SessionGeneration()

    private fun summary(message: String? = null) = ChatGptAccountState(
        connected = session != null, email = session?.email.orEmpty(),
        planEnabled = session?.scopes?.contains(OAuthProtocol.PLAN_SCOPE) == true,
        selectedModel = session?.selectedModel.orEmpty(), message = message,
        showPlanWelcome = session?.scopes?.contains(OAuthProtocol.PLAN_SCOPE) == true && !store.planWelcomeAcknowledged
    )
    fun acknowledgePlanWelcome() {
        store.acknowledgePlanWelcome()
        _state.update { it.copy(showPlanWelcome = false) }
    }

    /** Runs in the app scope so leaving Settings to open the browser does not destroy the listener. */
    suspend fun signIn(useDifferentAccount: Boolean = false) = mutex.withLock {
        withContext(Dispatchers.IO) {
            generation.advance()
            _state.update { it.copy(busy = true, signingIn = true, serviceReady = false,
                loginStep = LoginStep.BROWSER, message = null, diagnostic = null) }
            try {
                val registration = if (useDifferentAccount) null else store.lastRegistration()
                val server = ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))
                callbackServer = server
                server.use {
                    server.soTimeout = 1_000
                    val redirect = "http://127.0.0.1:${server.localPort}/auth/callback"
                    val stateValue = OAuthProtocol.randomValue()
                    val nonce = OAuthProtocol.randomValue()
                    val verifier = OAuthProtocol.randomValue()
                    val url = OAuthProtocol.authorizationUrl(redirect, store.hostId, stateValue, nonce, verifier,
                        registration?.first, session?.takeIf { !useDifferentAccount }?.idToken)
                    withContext(Dispatchers.Main) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                    val deadline = System.currentTimeMillis() + 5 * 60_000
                    var callback: OAuthProtocol.Callback? = null
                    while (callback == null && System.currentTimeMillis() < deadline) {
                        currentCoroutineContext().ensureActive()
                        val socket = try { server.accept() } catch (_: SocketTimeoutException) { continue }
                        socket.use { client ->
                            client.soTimeout = 2_000
                            val line = runCatching { client.getInputStream().bufferedReader().readLine() }.getOrNull()
                            val target = line?.takeIf { it.length < 16_384 && it.startsWith("GET ") }?.split(' ')?.getOrNull(1)
                            callback = runCatching {
                                OAuthProtocol.callback(target.orEmpty(), stateValue, registration?.first)
                            }.getOrNull()
                            val isAuthReturn = target?.startsWith("/auth/callback?") == true
                            val accepted = callback != null
                            val page = AccountReturnPage.html(accepted)
                            val bytes = page.toByteArray(Charsets.UTF_8)
                            runCatching {
                                client.getOutputStream().apply {
                                    write(("HTTP/1.1 ${if (accepted) "200 OK" else "400 Bad Request"}\r\n" +
                                        "Content-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\n" +
                                        "Cache-Control: no-store\r\nReferrer-Policy: no-referrer\r\n" +
                                        "Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; frame-ancestors 'none'\r\n" +
                                        "Connection: close\r\n\r\n").toByteArray())
                                    write(bytes); flush()
                                }
                            }
                            if (isAuthReturn && !accepted) throw ChatGptSignInRequired("授權未完成或回傳驗證失敗，請重試")
                        }
                    }
                    val result = callback ?: throw ChatGptSignInRequired("登入等待已逾時，請重試")
                    _state.update { it.copy(loginStep = LoginStep.VERIFY) }
                    if (registration == null) store.rememberPendingClient(result.clientId)
                    val tokens = postForm("${OAuthProtocol.ISSUER}/api/accounts/oauth/token", OAuthStage.CODE_EXCHANGE,
                        OAuthProtocol.codeExchangeForm(result, verifier, redirect))
                    val idToken = tokens.text("id_token")
                    val identity = OAuthProtocol.verifyIdToken(idToken, jwks(), result.clientId, nonce)
                    if (registration?.second != null && identity.subject != registration.second)
                        throw ChatGptSignInRequired("回傳帳號不符，請選擇「切換帳號」重新登入")
                    val next = tokenSession(tokens, result.clientId, identity.subject, identity.email, idToken)
                    currentCoroutineContext().ensureActive()
                    store.save(next)
                    session = next
                    _state.value = summary(if (OAuthProtocol.PLAN_SCOPE !in next.scopes)
                        "帳號已登入，但尚未授權使用 ChatGPT 額度。請重新登入確認授權。" else null)
                        .copy(busy = true, signingIn = true, loginStep = LoginStep.SERVICE)
                    if (OAuthProtocol.PLAN_SCOPE in next.scopes) loadModelsLocked()
                }
            } catch (e: CancellationException) {
                _state.value = summary(); throw e
            } catch (e: OAuthServiceError) {
                if (e.stage == OAuthStage.CODE_EXCHANGE && e.code in setOf("invalid_client", "unauthorized_client"))
                    store.clearRegistration()
                _state.value = summary(e.message).copy(diagnostic = e.diagnostic)
            } catch (e: Exception) {
                _state.value = summary(if (e is ChatGptSignInRequired)
                    e.message ?: "登入驗證失敗，請重試" else "無法完成登入，請確認網路與瀏覽器後重試")
            } finally {
                callbackServer?.close(); callbackServer = null
                _state.update { it.copy(busy = false, signingIn = false) }
            }
        }
    }

    fun cancelSignIn() { runCatching { callbackServer?.close() } }

    suspend fun authorizedSession(): ChatGptSession = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (_state.value.models.isEmpty()) loadModelsLocked()
            authorizedLocked()
        }
    }
    fun receipt(): Long = generation.current()
    suspend fun <T> withCurrentSession(receipt: Long, block: suspend () -> T): T = mutex.withLock {
        if (session == null || !generation.matches(receipt))
            throw ChatGptSignInRequired("帳號連線已變更，請重新查詢")
        block()
    }
    suspend fun invalidate(receipt: Long) = mutex.withLock {
        if (generation.matches(receipt)) {
            generation.advance(); store.clear(); session = null
            _state.value = summary("登入已失效，請重新登入")
        }
    }
    private suspend fun authorizedLocked(): ChatGptSession {
        var current = session ?: throw ChatGptSignInRequired()
        if (OAuthProtocol.PLAN_SCOPE !in current.scopes)
            throw ChatGptSignInRequired("請重新登入並授權使用 ChatGPT 額度")
        if (current.expiresAt <= System.currentTimeMillis() + 60_000) {
            if (current.refreshToken.isBlank()) throw ChatGptSignInRequired("登入已過期，請重新登入")
            val tokens = postForm("${OAuthProtocol.ISSUER}/api/accounts/oauth/token", OAuthStage.REFRESH, FormBody.Builder()
                .add("grant_type", "refresh_token").add("client_id", current.clientId)
                .add("refresh_token", current.refreshToken).add("resource", OAuthProtocol.RESOURCE).build())
            val idToken = tokens.text("id_token").ifBlank { current.idToken }
            if (tokens.text("id_token").isNotBlank()) {
                val identity = OAuthProtocol.verifyIdToken(idToken, jwks(), current.clientId, null)
                require(identity.subject == current.subject) { "更新登入時帳號不符" }
            }
            current = tokenSession(tokens, current.clientId, current.subject, current.email, idToken, current)
            store.save(current); session = current
        }
        if (OAuthProtocol.PLAN_SCOPE !in current.scopes) throw ChatGptSignInRequired("此帳號尚未啟用 ChatGPT 額度授權")
        return current
    }

    suspend fun refreshStatus() = mutex.withLock {
        withContext(Dispatchers.IO) {
            _state.update { it.copy(busy = true, message = null, diagnostic = null) }
            try { loadModelsLocked() }
            catch (e: CancellationException) { throw e }
            catch (e: OAuthServiceError) { _state.update { it.copy(serviceReady = false, message = e.message, diagnostic = e.diagnostic) } }
            catch (e: Exception) { _state.update { it.copy(serviceReady = false, message =
                if (e is ChatGptSignInRequired) e.message else "無法確認解讀服務，請重新登入或稍後重試") } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }
    private suspend fun loadModelsLocked() {
        val current = authorizedLocked()
        val request = Request.Builder().url("${OAuthProtocol.RESOURCE}/models")
            .header("Authorization", "Bearer ${current.accessToken}").build()
        val root = requestJson(request, OAuthStage.MODELS)
        val models = AccountModels.parse(root)
        val selected = AccountModels.select(models, current.selectedModel)
        session = current.copy(selectedModel = selected).also(store::save)
        _state.value = summary(if (models.isEmpty()) "此帳號目前沒有可用模型，請查看 ChatGPT 方案與額度設定" else null)
            .copy(models = models, serviceReady = models.isNotEmpty(), busy = _state.value.busy,
                signingIn = _state.value.signingIn, loginStep = LoginStep.SERVICE)
    }
    suspend fun selectModel(id: String) = mutex.withLock {
        require(_state.value.models.any { it.id == id })
        session?.copy(selectedModel = id)?.let { store.save(it); session = it }
        _state.update { it.copy(selectedModel = id) }
    }

    suspend fun signOut() {
      generation.advance()
      mutex.withLock {
        withContext(Dispatchers.IO) {
            val current = session
            store.clear(); session = null
            _state.value = summary().copy(busy = true)
            var revoked = current?.refreshToken.isNullOrBlank()
            try {
                if (!revoked && current != null) {
                    repeat(2) { attempt ->
                        if (!revoked) {
                            revoked = runCatching {
                                http.newCall(Request.Builder().url("${OAuthProtocol.ISSUER}/api/accounts/oauth/revoke")
                                    .post(FormBody.Builder().add("token", current.refreshToken)
                                        .add("token_type_hint", "refresh_token").add("client_id", current.clientId).build()).build())
                                    .execute().use { it.isSuccessful }
                            }.getOrDefault(false)
                            if (!revoked && attempt == 0) delay(1_000)
                        }
                    }
                }
            } finally {
                store.clear(); session = null
                _state.value = summary(if (revoked) "已登出" else
                    "本機已登出；遠端撤銷尚未確認，可到 ChatGPT 設定中解除連線")
            }
        }
      }
    }

    private fun tokenSession(tokens: JsonObject, clientId: String, sub: String, email: String, id: String,
                             previous: ChatGptSession? = null): ChatGptSession {
        val access = tokens.text("access_token")
        require(access.isNotBlank() && tokens.text("token_type").equals("Bearer", true)) { "登入未取得有效存取憑證" }
        val lifetime = tokens["expires_in"]?.jsonPrimitive?.longOrNull ?: 0L
        require(lifetime in 1..604_800) { "登入憑證期限無效" }
        val scopes = if (tokens.containsKey("scope")) tokens.text("scope").split(' ').filter { it.isNotBlank() }
            else previous?.scopes.orEmpty()
        return ChatGptSession(clientId, sub, email, access,
            tokens.text("refresh_token").ifBlank { previous?.refreshToken.orEmpty() }, id, scopes,
            System.currentTimeMillis() + lifetime * 1000, previous?.selectedModel.orEmpty())
    }
    private suspend fun jwks() = requestJson(Request.Builder().url("${OAuthProtocol.ISSUER}/.well-known/jwks.json").build(), OAuthStage.IDENTITY_KEYS)
    private suspend fun postForm(url: String, stage: OAuthStage, body: FormBody) = requestJson(Request.Builder().url(url).post(body).build(), stage)
    private suspend fun requestJson(request: Request, stage: OAuthStage): JsonObject = coroutineScope {
        val call = http.newCall(request)
        val cancellation = launch(start = CoroutineStart.UNDISPATCHED) {
            try { awaitCancellation() } finally { call.cancel() }
        }
        try {
            call.execute().use {
                if (!it.isSuccessful) throw OAuthServiceError.parse(stage, it.code, it.body?.string().orEmpty(), it.header("x-request-id"))
                json.parseToJsonElement(it.body?.string().orEmpty()).jsonObject
            }
        } finally { cancellation.cancel() }
    }
    private fun JsonObject.text(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
}
