package com.workcontrol.app.data.auth

import android.os.Build
import androidx.fragment.app.FragmentActivity
import com.google.gson.Gson
import com.workcontrol.app.data.prelo.LoginRequest
import com.workcontrol.app.data.prelo.MobileRefreshRequest
import com.workcontrol.app.data.prelo.MobileVerifyRequest
import com.workcontrol.app.data.prelo.MobileTokens
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException

sealed interface SessionPhase {
    data class Login(val notice: String? = null) : SessionPhase
    data object Code : SessionPhase
    data object SetupRequired : SessionPhase
    data object Unlock : SessionPhase
    data object Ready : SessionPhase
}

interface SessionController {
    val phase: StateFlow<SessionPhase>
    suspend fun start()
    suspend fun login(email: String, password: String)
    suspend fun verify(activity: FragmentActivity, code: String): Boolean
    suspend fun unlockAndRestore(activity: FragmentActivity): Boolean
    suspend fun logout(activity: FragmentActivity): Boolean
    fun sessionEnded()
}

@Singleton
class AuthSession @Inject constructor(
    private val api: PreloAuthApi,
    private val refreshStore: SecureRefreshStore,
    private val device: DevicePreferences,
    private val access: InMemoryAccessTokenStore,
    private val localUnlock: LocalUnlock,
    private val gson: Gson,
) : SessionController {
    private val refreshMutex = Mutex()
    private val mutablePhase = MutableStateFlow<SessionPhase>(SessionPhase.Login())
    override val phase: StateFlow<SessionPhase> = mutablePhase
    private var challenge: String? = null
    private var restoreAttempted = false

    override suspend fun start() {
        if (restoreAttempted) return
        restoreAttempted = true
        mutablePhase.value = if (withContext(Dispatchers.IO) { refreshStore.exists() }) SessionPhase.Unlock else SessionPhase.Login()
    }

    override suspend fun login(email: String, password: String) {
        val response = api.login(LoginRequest(email.trim(), password))
        challenge = response.challenge
        mutablePhase.value = when (response.nextStep) {
            "TOTP_REQUIRED" -> SessionPhase.Code
            "TOTP_SETUP_REQUIRED" -> SessionPhase.SetupRequired
            else -> SessionPhase.Login("Não foi possível iniciar o login.")
        }
    }

    override suspend fun verify(activity: FragmentActivity, code: String): Boolean {
        val pending = challenge ?: return false
        val tokens = api.verify(MobileVerifyRequest(pending, code.trim(), device.deviceId(), Build.MODEL))
        if (!localUnlock.authenticate(activity)) {
            runCatching { api.logout(MobileRefreshRequest(tokens.refreshToken, device.deviceId())) }
            mutablePhase.value = SessionPhase.Login("Desbloqueio local necessário para salvar a sessão.")
            return false
        }
        try {
            withContext(Dispatchers.IO) { refreshStore.write(tokens.refreshToken) }
        } catch (failure: Exception) {
            runCatching { api.logout(MobileRefreshRequest(tokens.refreshToken, device.deviceId())) }
            clearLocal("Não foi possível proteger a sessão neste aparelho.")
            return false
        }
        access.replace(tokens.accessToken)
        challenge = null
        mutablePhase.value = SessionPhase.Ready
        return true
    }

    override suspend fun unlockAndRestore(activity: FragmentActivity): Boolean {
        if (!localUnlock.authenticate(activity)) return false
        return refreshMutex.withLock { refreshLocked() != null }
    }

    /** Called from OkHttp Authenticator. The mutex collapses simultaneous 401s. */
    suspend fun refreshIfNeeded(tokenUsed: String?): String? = refreshMutex.withLock {
        val current = access.accessToken()
        if (current != null && current != tokenUsed) return@withLock current
        return@withLock refreshLocked()
    }

    private suspend fun refreshLocked(): String? {
        val old = try { withContext(Dispatchers.IO) { refreshStore.read() } }
        catch (_: Exception) { mutablePhase.value = SessionPhase.Unlock; return null }
        if (old == null) { clearLocal(); return null }
        val next = try { api.refresh(MobileRefreshRequest(old, device.deviceId())) }
        catch (http: HttpException) {
            if (http.code() == 401) {
                val code = runCatching { gson.fromJson(http.response()?.errorBody()?.charStream(), ServerError::class.java)?.code }.getOrNull()
                clearLocal(if (code == "refresh_reused") "Sua sessão foi encerrada por segurança." else null)
            }
            return null
        }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { return null }
        // A consumed refresh cannot be retried. If durable rotation fails, discard the session.
        try { withContext(Dispatchers.IO) { refreshStore.write(next.refreshToken) } }
        catch (_: Exception) { clearLocal("Não foi possível salvar a nova sessão."); return null }
        access.replace(next.accessToken)
        mutablePhase.value = SessionPhase.Ready
        return next.accessToken
    }

    override suspend fun logout(activity: FragmentActivity): Boolean {
        if (!localUnlock.authenticate(activity)) return false
        val old = runCatching { withContext(Dispatchers.IO) { refreshStore.read() } }.getOrNull()
        val token = access.accessToken()
        if (token != null) runCatching { api.deletePushToken("Bearer $token") }
        if (old != null) {
            try { api.logout(MobileRefreshRequest(old, device.deviceId())) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { return false }
        }
        clearLocal()
        return true
    }

    override fun sessionEnded() = clearLocal()

    private fun clearLocal(notice: String? = null) {
        access.clear()
        runCatching { refreshStore.clear() }
        challenge = null
        mutablePhase.value = SessionPhase.Login(notice)
    }

    private data class ServerError(val code: String?)
}
