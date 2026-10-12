package com.zwds.today.data.auth

import android.content.Context
import java.security.KeyStore

/** Remove the retired connector's local credential only. Family/history databases stay intact. */
object LegacyCredentialCleanup {
    fun run(context: Context) {
        val prefs = context.getSharedPreferences("openrouter_account", Context.MODE_PRIVATE)
        if (prefs.all.isNotEmpty()) check(prefs.edit().clear().commit())
        runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.let { keys ->
                if (keys.containsAlias("zwds_openrouter_v1")) keys.deleteEntry("zwds_openrouter_v1")
            }
        }
    }
}
