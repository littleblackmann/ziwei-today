package com.zwds.today

import android.app.Application
import com.zwds.today.di.ServiceLocator

class ZwdsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
