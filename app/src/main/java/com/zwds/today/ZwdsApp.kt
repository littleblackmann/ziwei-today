package com.zwds.today

import android.app.Application
import kotlinx.coroutines.launch
import com.zwds.today.di.ServiceLocator

class ZwdsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        ServiceLocator.updateRepository.schedule()
        ServiceLocator.applicationScope.launch {
            ServiceLocator.userPreferences.removeLegacyApiKey()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.zwds.today.data.auth.LegacyCredentialCleanup.run(this@ZwdsApp)
            }
        }
    }
}
