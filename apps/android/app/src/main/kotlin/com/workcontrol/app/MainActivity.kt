package com.workcontrol.app

import android.os.Bundle
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.workcontrol.app.core.designsystem.ThemeMode
import com.workcontrol.app.core.designsystem.ThemePreference
import com.workcontrol.app.core.designsystem.WorkControlTheme
import kotlinx.coroutines.launch
import com.workcontrol.app.data.auth.AuthSession
import com.workcontrol.app.data.auth.DevicePreferences
import com.workcontrol.app.data.auth.DeviceSecurity
import com.workcontrol.app.data.prelo.PreloApi
import com.workcontrol.app.data.prelo.PreloResourceApi
import com.workcontrol.app.data.prelo.PreloEvents
import com.workcontrol.app.data.push.PushRouting
import com.workcontrol.app.data.push.PushRegistrar
import com.workcontrol.app.data.prelo.ApprovalDecisions
import com.workcontrol.app.feature.session.SessionScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var session: AuthSession
    @Inject lateinit var prelo: PreloApi
    @Inject lateinit var resources: PreloResourceApi
    @Inject lateinit var preferences: DevicePreferences
    @Inject lateinit var deviceSecurity: DeviceSecurity
    @Inject lateinit var decisions: ApprovalDecisions
    @Inject lateinit var events: PreloEvents
    @Inject lateinit var pushRouting: PushRouting
    @Inject lateinit var pushRegistrar: PushRegistrar
    @Inject lateinit var appearance: ThemePreference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pushRouting.accept(intent)
        enableEdgeToEdge()
        setContent {
            val mode by appearance.mode.collectAsState(initial = ThemeMode.SYSTEM)
            val scope = rememberCoroutineScope()
            WorkControlTheme(mode) {
                SessionScreen(this, session, prelo, resources, preferences, decisions, events,
                    pushRouting, pushRegistrar, deviceSecurity,
                    themeMode = mode, onThemeMode = { scope.launch { appearance.set(it) } })
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pushRouting.accept(intent)
    }
}
