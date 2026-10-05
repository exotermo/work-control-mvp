package com.workcontrol.app.feature.session

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.gson.Gson
import com.workcontrol.app.core.components.Clipping
import com.workcontrol.app.core.components.ClippingFooter
import com.workcontrol.app.core.components.ErrorClipping
import com.workcontrol.app.core.components.GhostButton
import com.workcontrol.app.core.components.Headline
import com.workcontrol.app.core.components.InkButton
import com.workcontrol.app.core.components.InkField
import com.workcontrol.app.core.components.InkLink
import com.workcontrol.app.core.components.Kicker
import com.workcontrol.app.core.components.Masthead
import com.workcontrol.app.core.components.PaperBackground
import com.workcontrol.app.core.components.PressSkeleton
import com.workcontrol.app.core.designsystem.Prelo
import com.workcontrol.app.core.designsystem.PreloType
import com.workcontrol.app.core.designsystem.ThemeMode
import com.workcontrol.app.data.auth.AuthSession
import com.workcontrol.app.data.auth.SessionController
import com.workcontrol.app.data.auth.DevicePreferences
import com.workcontrol.app.data.auth.DeviceSecurity
import com.workcontrol.app.data.auth.SessionPhase
import com.workcontrol.app.data.prelo.Me
import com.workcontrol.app.data.prelo.MobileSession
import com.workcontrol.app.data.prelo.PreloApi
import com.workcontrol.app.data.prelo.PreloResourceApi
import com.workcontrol.app.data.prelo.PreloEvents
import com.workcontrol.app.data.prelo.EventFeed
import com.workcontrol.app.data.prelo.ApprovalDecisions
import com.workcontrol.app.feature.prelo.PreloDashboard
import com.workcontrol.app.data.push.PushRouting
import com.workcontrol.app.data.push.PushRegistrar
import com.workcontrol.app.data.push.PushControl
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun SessionScreen(activity: FragmentActivity, session: SessionController, api: PreloApi,
    resources: PreloResourceApi, preferences: DevicePreferences, decisions: ApprovalDecisions,
    events: EventFeed, pushRouting: PushRouting, pushRegistrar: PushControl,
    deviceSecurity: DeviceSecurity, themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeMode: (ThemeMode) -> Unit = {}) {
    val phase by session.phase.collectAsState()
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var deviceSecure by remember { mutableStateOf(deviceSecurity.isDeviceSecure()) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var me by remember { mutableStateOf<Me?>(null) }
    var devices by remember { mutableStateOf<List<MobileSession>>(emptyList()) }
    var pushWanted by remember { mutableStateOf(true) }
    val serverPush by pushRegistrar.serverEnabled.collectAsState()
    var notificationsAllowed by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = granted || Build.VERSION.SDK_INT < 33
    }
    LaunchedEffect(Unit) { session.start() }
    LaunchedEffect(phase) {
        if (phase == SessionPhase.Code) deviceSecure = deviceSecurity.isDeviceSecure()
    }
    DisposableEffect(activity, deviceSecurity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) deviceSecure = deviceSecurity.isDeviceSecure()
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(phase) {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (phase == SessionPhase.Ready && event == Lifecycle.Event.ON_START) events.start()
            if (event == Lifecycle.Event.ON_STOP) events.stop()
        }
        lifecycle.addObserver(observer)
        if (phase == SessionPhase.Ready && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) events.start()
        onDispose { lifecycle.removeObserver(observer); events.stop() }
    }
    LaunchedEffect(phase) {
        if (phase == SessionPhase.Ready) {
            try { me = api.me() }
            catch (failure: Exception) {
                if (failure is HttpException && failure.code() == 401) session.sessionEnded()
                else error = authError(failure)
            }
            pushWanted = preferences.pushWanted()
            pushRegistrar.onLogin()
        } else { me = null; devices = emptyList() }
    }
    fun submit(block: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true; error = null
            try { block() } catch (failure: Exception) { error = authError(failure) }
            finally { busy = false }
        }
    }
    if (phase == SessionPhase.Ready) {
        val profile = me
        if (profile == null) {
            PaperBackground(Modifier.fillMaxSize()) {
                Column(Modifier.statusBarsPadding().padding(16.dp)) {
                    Masthead(live = false)
                    error?.let { ErrorClipping(it, Modifier.padding(top = 12.dp)) } ?: PressSkeleton(Modifier.padding(top = 16.dp))
                }
            }
        } else {
            PreloDashboard(profile, resources, preferences, decisions, events, pushRouting, Modifier.fillMaxSize(),
                homeNotice = if (!notificationsAllowed) ({
                    NotificationNotice { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                }) else null,
                account = {
                    AccountContent(
                        serverPush = serverPush, pushWanted = pushWanted, notificationsAllowed = notificationsAllowed,
                        onAllowNotifications = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        onTogglePush = { submit {
                            if (pushRegistrar.setWanted(!pushWanted)) pushWanted = !pushWanted
                            else error = "Não foi possível alterar as notificações no servidor."
                        } },
                        themeMode = themeMode, onThemeMode = onThemeMode,
                        devices = devices, onLoadDevices = { submit { devices = api.sessions() } },
                        onRevoke = { item -> submit {
                            api.revokeSession(item.id)
                            if (item.current) session.sessionEnded() else devices = api.sessions()
                        } },
                        onLogout = { submit {
                            if (!session.logout(activity)) error = "Não foi possível encerrar a sessão no servidor. Tente novamente."
                        } },
                        busy = busy, error = error,
                    )
                })
        }
        return
    }

    // Signed out: the front page of the "access edition", with each step as a clipping.
    PaperBackground(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            AccessMasthead()
            when (val current = phase) {
                is SessionPhase.Login -> Clipping(seed = 11) {
                    Kicker("Acesso")
                    Headline("Entre na redação")
                    current.notice?.let { Text(it, Modifier.padding(bottom = 8.dp), style = PreloType.Body, color = Prelo.colors.bad) }
                    InkField(email, { email = it }, "E-mail", Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
                    InkField(password, { password = it }, "Senha", Modifier.fillMaxWidth().padding(top = 8.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done))
                    InkButton("Entrar", onClick = { submit { session.login(email, password); password = "" } },
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp), enabled = !busy)
                }
                SessionPhase.Code -> if (!deviceSecure) Clipping(seed = 12) {
                    Kicker("Segurança do aparelho")
                    Headline("Falta um bloqueio de tela")
                    Text("Configure um bloqueio de tela (PIN, padrão ou digital) neste aparelho para entrar no Prelo",
                        style = PreloType.Body, color = Prelo.colors.text)
                    GhostButton("Abrir configurações de segurança", onClick = {
                        runCatching { activity.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
                            .onFailure { error = "Não foi possível abrir as configurações de segurança." }
                    }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
                } else Clipping(seed = 13) {
                    Kicker("Telegrama · verificação em duas etapas")
                    Headline("Confirme que é você")
                    Text("Digite o código de verificação ou um código de recuperação.", style = PreloType.Body, color = Prelo.colors.text)
                    InkField(code, { code = it }, "Código", Modifier.fillMaxWidth().padding(top = 10.dp), mono = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done))
                    InkButton("Verificar", onClick = {
                        if (!deviceSecurity.isDeviceSecure()) deviceSecure = false
                        else submit { session.verify(activity, code); code = "" }
                    }, modifier = Modifier.fillMaxWidth().padding(top = 18.dp), enabled = !busy)
                }
                SessionPhase.SetupRequired -> Clipping(seed = 14) {
                    Kicker("Primeiro acesso")
                    Headline("Falta a verificação em duas etapas")
                    Text("Configure a verificação em duas etapas pelo navegador.", style = PreloType.Body, color = Prelo.colors.text)
                    ClippingFooter { InkLink("Voltar ao login", onClick = { session.sessionEnded() }) }
                }
                SessionPhase.Unlock -> Clipping(seed = 15) {
                    Kicker("Sessão guardada neste aparelho")
                    Headline("Bem-vindo de volta")
                    Text("Confirme no aparelho para desbloquear sua sessão.", style = PreloType.Body, color = Prelo.colors.text)
                    InkButton("Desbloquear", onClick = { submit { session.unlockAndRestore(activity) } },
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp), enabled = !busy)
                }
                SessionPhase.Ready -> Unit
            }
            error?.let { Text(it, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = Prelo.colors.bad) }
        }
    }
}

private fun authError(failure: Exception): String {
    if (failure !is HttpException) return "Não foi possível conectar. Tente novamente."
    val code = runCatching {
        Gson().fromJson(failure.response()?.errorBody()?.charStream(), AuthError::class.java)?.code
    }.getOrNull()
    return when (code) {
        "invalid_credentials" -> "E-mail ou senha inválidos."
        "invalid_code", "invalid_or_expired_token" -> "Código inválido ou expirado."
        "account_locked", "rate_limited" -> "Muitas tentativas. Aguarde e tente novamente."
        "refresh_reused" -> "Sua sessão foi encerrada por segurança."
        else -> when (failure.code()) {
            401 -> "Entre novamente para continuar."
            403 -> "Sem acesso."
            404 -> "Recurso não encontrado."
            else -> "Não foi possível concluir a operação."
        }
    }
}

private data class AuthError(val code: String?)
