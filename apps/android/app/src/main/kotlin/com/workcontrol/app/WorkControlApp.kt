package com.workcontrol.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.workcontrol.app.data.push.createPreloAlertsChannel

@HiltAndroidApp
class WorkControlApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createPreloAlertsChannel(this)
    }
}
