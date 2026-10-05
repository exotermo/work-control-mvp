package com.workcontrol.app

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.fragment.app.FragmentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.workcontrol.app.data.auth.DevicePreferences
import com.workcontrol.app.data.auth.DeviceSecurity
import com.workcontrol.app.data.auth.SessionController
import com.workcontrol.app.data.auth.SessionPhase
import com.workcontrol.app.data.prelo.ApprovalDecisions
import com.workcontrol.app.data.prelo.EventFeed
import com.workcontrol.app.data.prelo.PreloApi
import com.workcontrol.app.data.prelo.PreloEvent
import com.workcontrol.app.data.prelo.PreloResourceApi
import com.workcontrol.app.data.push.PushControl
import com.workcontrol.app.data.push.PushRouting
import com.workcontrol.app.feature.session.SessionScreen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@RunWith(AndroidJUnit4::class)
class PreloFlowInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var server: MockWebServer

    @Before fun setUp() { server = MockWebServer().also { it.start() } }
    @After fun tearDown() { server.shutdown() }

    @Test fun loginWithTotpOpensTasksFromPrelo() {
        showScreen(tasksForbidden = false)
        login()
        compose.onNodeWithText("Tarefas").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Inspecionar deploy · QUEUED").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Inspecionar deploy · QUEUED").performScrollTo().assertIsDisplayed()
        assertTrue(server.requestCount >= 3)
    }

    @Test fun forbiddenTasksShowServerDecision() {
        showScreen(tasksForbidden = true)
        login()
        compose.onNodeWithText("Tarefas").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Sem acesso.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Sem acesso.").performScrollTo().assertIsDisplayed()
    }

    @Test fun insecureDeviceShowsSettingsBeforeTotpVerification() {
        showScreen(tasksForbidden = false, secureDevice = false)
        compose.onNodeWithText("E-mail").performTextInput("user@example.com")
        compose.onNodeWithText("Senha").performTextInput("example-password")
        compose.onNodeWithText("Entrar").performClick()

        compose.onNodeWithText("Configure um bloqueio de tela (PIN, padrão ou digital) neste aparelho para entrar no Prelo")
            .assertIsDisplayed()
        compose.onNodeWithText("Abrir configurações de segurança").assertIsDisplayed()
        compose.onNodeWithText("Verificar").assertDoesNotExist()
        assertEquals(0, server.requestCount)
    }

    private fun login() {
        compose.onNodeWithText("E-mail").performTextInput("user@example.com")
        compose.onNodeWithText("Senha").performTextInput("example-password")
        compose.onNodeWithText("Entrar").performClick()
        compose.onNodeWithText("Código").performTextInput("123456")
        compose.onNodeWithText("Verificar").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Tarefas").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun showScreen(tasksForbidden: Boolean, secureDevice: Boolean = true) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val body = when (request.path?.substringBefore('?')) {
                    "/api/v1/me" -> """{"userId":"u1","email":"user@example.com","role":"OPERATOR","scopes":[],"workspaceId":"w1","workspaceName":"Prelo","projects":[{"id":"p1","name":"Loja","clientId":null}],"session":{"kind":"mobile","deviceId":"d1","deviceName":"emulator"}}"""
                    "/api/v1/home" -> """{"recent":[],"pending":[]}"""
                    "/api/v1/tasks" -> if (tasksForbidden) return MockResponse().setResponseCode(403)
                        .addHeader("Content-Type", "application/json").setBody("""{"code":"forbidden"}""")
                        else """[{"id":"t1","description":"Inspecionar deploy","status":"QUEUED","agentId":"a1"}]"""
                    "/api/v1/agents" -> "[]"
                    else -> "[]"
                }
                return MockResponse().addHeader("Content-Type", "application/json").setBody(body)
            }
        }
        val retrofit = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create()).build()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val session = FakeSession()
        val api = retrofit.create(PreloApi::class.java)
        val resources = retrofit.create(PreloResourceApi::class.java)
        compose.runOnUiThread {
            compose.activity.setContent {
                SessionScreen(compose.activity, session, api, resources, DevicePreferences(context),
                    ApprovalDecisions(resources), FakeEvents(), PushRouting(), FakePush(),
                    DeviceSecurity { secureDevice })
            }
        }
    }
}

private class FakeSession : SessionController {
    private val state = MutableStateFlow<SessionPhase>(SessionPhase.Login())
    override val phase: StateFlow<SessionPhase> = state
    override suspend fun start() = Unit
    override suspend fun login(email: String, password: String) { state.value = SessionPhase.Code }
    override suspend fun verify(activity: FragmentActivity, code: String): Boolean {
        state.value = SessionPhase.Ready; return true
    }
    override suspend fun unlockAndRestore(activity: FragmentActivity) = true
    override suspend fun logout(activity: FragmentActivity): Boolean { state.value = SessionPhase.Login(); return true }
    override fun sessionEnded() { state.value = SessionPhase.Login() }
}

private class FakeEvents : EventFeed {
    override val connected: StateFlow<Boolean> = MutableStateFlow(false)
    override val events: SharedFlow<PreloEvent> = MutableSharedFlow()
    override fun start() = Unit
    override fun stop() = Unit
}

private class FakePush : PushControl {
    override val serverEnabled: StateFlow<Boolean> = MutableStateFlow(false)
    override suspend fun onLogin() = Unit
    override suspend fun setWanted(value: Boolean) = true
}
