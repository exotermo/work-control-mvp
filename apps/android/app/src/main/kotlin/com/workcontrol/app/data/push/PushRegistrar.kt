package com.workcontrol.app.data.push

import com.google.firebase.messaging.FirebaseMessaging
import com.workcontrol.app.BuildConfig
import com.workcontrol.app.data.auth.AuthSession
import com.workcontrol.app.data.auth.DevicePreferences
import com.workcontrol.app.data.auth.SessionPhase
import com.workcontrol.app.data.prelo.PreloResourceApi
import com.workcontrol.app.data.prelo.PushToken
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Singleton
class PushRegistrar @Inject constructor(
    private val api: PreloResourceApi,
    private val session: AuthSession,
    private val preferences: DevicePreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingToken: String? = null
    private val mutableServerEnabled = MutableStateFlow(false)
    val serverEnabled: StateFlow<Boolean> = mutableServerEnabled

    fun onNewToken(token: String) {
        pendingToken = token
        if (BuildConfig.HAS_FIREBASE_CONFIG && session.phase.value == SessionPhase.Ready) scope.launch { register(token) }
    }

    suspend fun onLogin() {
        if (!BuildConfig.HAS_FIREBASE_CONFIG || !preferences.pushWanted()) return
        val token = pendingToken ?: suspendCancellableCoroutine<String?> { continuation ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (continuation.isActive) continuation.resume(if (task.isSuccessful) task.result else null)
            }
        }
        if (token != null) register(token)
    }

    private suspend fun register(token: String) {
        if (session.phase.value != SessionPhase.Ready || !preferences.pushWanted()) {
            pendingToken = token
            return
        }
        val response = runCatching { api.registerPush(PushToken(token)) }.getOrNull() ?: return
        mutableServerEnabled.value = response.pushEnabled
        pendingToken = null
    }

    suspend fun setWanted(value: Boolean): Boolean {
        if (!value) {
            try { api.deletePush() } catch (_: Exception) { return false }
            preferences.setPushWanted(false)
            mutableServerEnabled.value = false
            return true
        }
        preferences.setPushWanted(true)
        onLogin()
        return true
    }
}
