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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.gson.Gson
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
    deviceSecurity: DeviceSecurity) {
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
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
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
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Prelo Control")
        when (val current = phase) {
            is SessionPhase.Login -> {
                current.notice?.let { Text(it) }
                OutlinedTextField(email, { email = it }, label = { Text("E-mail") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text("Senha") },
                    visualTransformation = PasswordVisualTransformation(), singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Button(onClick = { submit { session.login(email, password); password = "" } }, enabled = !busy) {
                    Text("Entrar")
                }
            }
            SessionPhase.Code -> {
                if (!deviceSecure) {
                    Text("Configure um bloqueio de tela (PIN, padrão ou digital) neste aparelho para entrar no Prelo")
                    Button(onClick = {
                        runCatching { activity.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
                            .onFailure { error = "Não foi possível abrir as configurações de segurança." }
                    }) {
                        Text("Abrir configurações de segurança")
                    }
                } else {
                    Text("Digite o código de verificação ou um código de recuperação.")
                    OutlinedTextField(code, { code = it }, label = { Text("Código") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        if (!deviceSecurity.isDeviceSecure()) deviceSecure = false
                        else submit { session.verify(activity, code); code = "" }
                    }, enabled = !busy) {
                        Text("Verificar")
                    }
                }
            }
            SessionPhase.SetupRequired -> Text("Configure a verificação em duas etapas pelo navegador.")
            SessionPhase.Unlock -> {
                Text("Confirme no aparelho para desbloquear sua sessão.")
                Button(onClick = { submit { session.unlockAndRestore(activity) } }, enabled = !busy) {
                    Text("Desbloquear")
                }
            }
            SessionPhase.Ready -> {
                if (me == null) CircularProgressIndicator()
                me?.let { profile ->
                    PreloDashboard(profile, resources, preferences, decisions, events, pushRouting, Modifier.weight(1f))
                    if (Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        Text("Ative notificações para receber alertas de aprovações e deploys.")
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                            Text("Permitir notificações")
                        }
                    }
                    if (!serverPush) Text("Notificações ainda não ativadas no servidor.")
                    Button(onClick = { submit {
                        if (pushRegistrar.setWanted(!pushWanted)) pushWanted = !pushWanted
                        else error = "Não foi possível alterar as notificações no servidor."
                    } }, enabled = !busy) { Text(if (pushWanted) "Desligar notificações" else "Ligar notificações") }
                    Button(onClick = { submit { devices = api.sessions() } }, enabled = !busy) {
                        Text("Aparelhos conectados")
                    }
                    LazyColumn(Modifier.weight(1f)) {
                        items(devices, key = { it.id }) { item ->
                            Text("${item.deviceName} · ${item.platform}${if (item.current) " · este aparelho" else ""}")
                            Button(onClick = { submit {
                                api.revokeSession(item.id)
                                if (item.current) session.sessionEnded() else devices = api.sessions()
                            } }, enabled = !busy) { Text("Revogar") }
                        }
                    }
                    Button(onClick = { submit {
                        if (!session.logout(activity)) error = "Não foi possível encerrar a sessão no servidor. Tente novamente."
                    } }, enabled = !busy) {
                        Text("Sair")
                    }
                }
            }
        }
        error?.let { Text(it) }
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
