package com.workcontrol.app

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.fragment.app.FragmentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso
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
        compose.onNodeWithText("Tarefas").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Inspecionar deploy").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Inspecionar deploy").assertIsDisplayed()
        compose.onNodeWithContentDescription("Situação: NA FILA").assertIsDisplayed()
        assertTrue(server.requestCount >= 3)
    }

    @Test fun forbiddenTasksShowServerDecision() {
        showScreen(tasksForbidden = true)
        login()
        compose.onNodeWithText("Tarefas").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Sem acesso.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Sem acesso.").assertIsDisplayed()
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

    @Test fun backFromTotpReturnsToEmailAndPassword() {
        showScreen(tasksForbidden = false)
        compose.onNodeWithText("E-mail").performTextInput("user@example.com")
        compose.onNodeWithText("Senha").performTextInput("example-password")
        compose.onNodeWithText("Entrar").performClick()
        compose.onNodeWithText("Código").assertIsDisplayed()
        Espresso.pressBack()
        compose.onNodeWithText("E-mail").assertIsDisplayed()
        compose.onNodeWithText("Senha").assertIsDisplayed()
    }

    @Test fun projectTaskBackReturnsToHome() {
        showScreen(tasksForbidden = false)
        login()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Nada esperando por você agora.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasScrollAction()).performScrollToIndex(4)
        compose.onNodeWithContentDescription("Abrir projeto Loja").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("DOSSIÊ · PRELO").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasScrollAction()).performScrollToIndex(3)
        compose.onNode(hasScrollAction()).performScrollToIndex(5)
        compose.onNodeWithContentDescription("Abrir seção Tarefas").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Inspecionar deploy").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Inspecionar deploy").performClick()
        Espresso.pressBack()
        compose.onNodeWithText("Pauta de trabalho").assertExists()
        Espresso.pressBack()
        compose.onNode(hasScrollAction()).performScrollToIndex(3)
        compose.onNodeWithText("NÚMEROS DA EDIÇÃO").assertExists()
        Espresso.pressBack()
        compose.onNode(hasScrollAction()).performScrollToIndex(0)
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Nada esperando por você agora.").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(compose.onAllNodesWithText("Nada esperando por você agora.").fetchSemanticsNodes().isNotEmpty())
    }

    @Test fun taskSheetBackOnlyClosesSheet() {
        showScreen(tasksForbidden = false)
        login()
        compose.onNodeWithText("＋ Nova tarefa").performClick()
        compose.onNodeWithText("O que o agente deve fazer?").assertIsDisplayed()
        Espresso.pressBack()
        compose.onNodeWithText("＋ Nova tarefa").assertIsDisplayed()
        compose.onNodeWithText("O que o agente deve fazer?").assertDoesNotExist()
    }

    @Test fun clientDossierShowsContactsAndTimeline() {
        showScreen(tasksForbidden = false)
        login()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Nada esperando por você agora.").fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(5_000) {
            runCatching { compose.onNode(hasScrollAction()).performScrollToIndex(6) }.isSuccess &&
                compose.onAllNodes(hasContentDescription("Abrir cliente Ana da Loja")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Abrir cliente Ana da Loja").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("DOSSIÊ · CLIENTE").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Ana da Loja").assertIsDisplayed()
        compose.waitUntil(5_000) { runCatching { compose.onNode(hasScrollAction()).performScrollToIndex(4) }.isSuccess }
        compose.onNodeWithText("EMAIL: ana@example.com").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToIndex(7)
        compose.onNodeWithText("Criou projeto Loja").assertExists()
    }

    @Test fun clientsForbiddenShowsNoAccess() {
        showScreen(tasksForbidden = false, clientsForbidden = true)
        login()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Sem acesso.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Sem acesso.").assertIsDisplayed()
    }

    private fun login() {
        compose.onNodeWithText("E-mail").performTextInput("user@example.com")
        compose.onNodeWithText("Senha").performTextInput("example-password")
        compose.onNodeWithText("Entrar").performClick()
        compose.onNodeWithText("Código").performTextInput("123456")
        compose.onNodeWithText("Verificar").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Tarefas").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun showScreen(tasksForbidden: Boolean, secureDevice: Boolean = true, clientsForbidden: Boolean = false) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val body = when (request.path?.substringBefore('?')) {
                    "/api/v1/me" -> """{"userId":"u1","email":"user@example.com","role":"OPERATOR","scopes":[],"workspaceId":"w1","workspaceName":"Prelo","projects":[{"id":"p1","name":"Loja","clientId":null}],"session":{"kind":"mobile","deviceId":"d1","deviceName":"emulator"}}"""
                    "/api/v1/home" -> """{"recent":[],"pending":[]}"""
                    "/api/v1/projects" -> """[{"id":"p1","name":"Loja","memberCount":2}]"""
                    "/api/v1/clients" -> if (clientsForbidden) return MockResponse().setResponseCode(403)
                        .addHeader("Content-Type", "application/json").setBody("""{"code":"forbidden"}""")
                        else """[{"id":"c1","name":"Ana da Loja","company":"Loja","status":"ACTIVE","projectCount":1}]"""
                    "/api/v1/clients/c1" -> """{"id":"c1","name":"Ana da Loja","status":"ACTIVE","stage":"REPLIED","contacts":[{"id":"cc1","kind":"EMAIL","value":"ana@example.com","isPrimary":true}],"projects":[{"id":"p1","name":"Loja"}]}"""
                    "/api/v1/clients/c1/timeline" -> """[{"kind":"PROJECT","id":"p1","title":"Criou projeto Loja","detail":"Primeiro projeto","status":"ACTIVE","projectId":"p1","at":"2026-10-06T10:00:00Z"}]"""
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
