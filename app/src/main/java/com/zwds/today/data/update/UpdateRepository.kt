package com.zwds.today.data.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.zwds.today.BuildConfig
import com.zwds.today.MainActivity
import com.zwds.today.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class UpdateState(val checking: Boolean = false, val latest: ReleaseInfo? = null,
                       val prompt: ReleaseInfo? = null, val enabled: Boolean = true,
                       val message: String? = null)
class UpdateRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("app_updates", Context.MODE_PRIVATE)
    private val http = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).callTimeout(25, TimeUnit.SECONDS).build()
    private val mutex = Mutex()
    private val savedRelease = runCatching {
        Json.decodeFromString<ReleaseInfo>(prefs.getString("release", null) ?: "null")
    }.getOrNull()?.takeIf {
        (AppVersion.parse(it.tag) ?: AppVersion(0, 0, 0)) > (AppVersion.parse(BuildConfig.VERSION_NAME) ?: AppVersion(0, 0, 0)) &&
            it.pageUrl == "${ReleaseInfo.RELEASES_URL}/tag/${it.tag}"
    }
    private val _state = MutableStateFlow(UpdateState(enabled = prefs.getBoolean("enabled", true), latest = savedRelease,
        prompt = savedRelease?.takeIf { prefs.getBoolean("enabled", true) && prefs.getString("dismissed", null) != it.tag }))
    val state = _state.asStateFlow()

    suspend fun check(manual: Boolean = false, background: Boolean = false): ReleaseInfo? = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (background && !_state.value.enabled) return@withContext null
            if (!manual && (!_state.value.enabled ||
                        System.currentTimeMillis() - prefs.getLong("checked_at", 0) < TimeUnit.HOURS.toMillis(6)))
                return@withContext _state.value.latest
            _state.value = _state.value.copy(checking = true, message = null)
            try {
                val request = Request.Builder().url("https://api.github.com/repos/${ReleaseInfo.REPO}/releases/latest")
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "ZiweiToday/${BuildConfig.VERSION_NAME}").build()
                val release = http.newCall(request).execute().use {
                    if (it.code == 404) null
                    else {
                        check(it.isSuccessful) { "更新服務暫時無法使用，請稍後再試" }
                        ReleaseInfo.parse(Json.parseToJsonElement(it.body?.string().orEmpty()).jsonObject, BuildConfig.VERSION_NAME)
                    }
                }
                currentCoroutineContext().ensureActive()
                prefs.edit().putLong("checked_at", System.currentTimeMillis()).apply()
                prefs.edit().putString("release", release?.let { Json.encodeToString(it) }).apply()
                _state.value = _state.value.copy(latest = release,
                    prompt = release?.takeIf { prefs.getString("dismissed", null) != it.tag },
                    message = if (release == null) "目前沒有可安裝的新版本" else "找到新版本 ${release.tag}")
                if (background && release != null) notifyRelease(release)
                release
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                _state.value = _state.value.copy(message = "無法檢查更新，請確認網路後重試")
                null
            } finally { _state.value = _state.value.copy(checking = false) }
        }
    }
    fun dismiss(tag: String) {
        prefs.edit().putString("dismissed", tag).apply()
        _state.value = _state.value.copy(prompt = null)
    }
    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("enabled", enabled).apply()
        _state.value = _state.value.copy(enabled = enabled, prompt = if (enabled) _state.value.prompt else null)
        schedule()
    }
    fun schedule() {
        val manager = WorkManager.getInstance(context)
        if (!_state.value.enabled) { manager.cancelUniqueWork("app-release-check"); return }
        manager.enqueueUniquePeriodicWork("app-release-check", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<UpdateCheckWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build())
    }
    private fun notifyRelease(release: ReleaseInfo) {
        if (!_state.value.enabled) return
        if (prefs.getString("notified", null) == release.tag || prefs.getString("dismissed", null) == release.tag) return
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val system = context.getSystemService(NotificationManager::class.java)
        system.createNotificationChannel(NotificationChannel("app_updates", "版本更新", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = PendingIntent.getActivity(context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            manager.notify(1200, NotificationCompat.Builder(context, "app_updates")
                .setSmallIcon(R.drawable.ic_update).setContentTitle("今日紫微 ${release.tag} 已發布")
                .setContentText("點一下查看更新內容與下載新版").setContentIntent(intent).setAutoCancel(true).build())
            prefs.edit().putString("notified", release.tag).apply()
        } catch (_: SecurityException) { /* User may revoke permission after the check. */ }
    }
}
class UpdateCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        com.zwds.today.di.ServiceLocator.updateRepository.check(manual = true, background = true)
        return Result.success()
    }
}
