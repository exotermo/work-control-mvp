package com.workcontrol.app

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.workcontrol.app.core.designsystem.ThemeMode
import com.workcontrol.app.core.designsystem.WorkControlTheme
import com.workcontrol.app.data.auth.DevicePreferences
import com.workcontrol.app.data.auth.DeviceSecurity
import com.workcontrol.app.data.prelo.ApprovalDecisions
import com.workcontrol.app.data.prelo.PreloApi
import com.workcontrol.app.data.prelo.PreloResourceApi
import com.workcontrol.app.data.push.PushRouting
import com.workcontrol.app.feature.session.SessionScreen
import java.io.File
import java.time.OffsetDateTime
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Walks every screen with realistic data in both editions and saves full-screen captures to
 * <external files>/tour/<edition>/NN-name.png (adb pull them to review the design). Also a smoke
 * test: every section renders its content from the API without crashing.
 */
@RunWith(AndroidJUnit4::class)
class DesignTourTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var server: MockWebServer

    @Before fun setUp() { server = MockWebServer().also { it.dispatcher = TourData; it.start() } }
    @After fun tearDown() { server.shutdown() }

    @Test fun paperEdition() = tour(ThemeMode.PAPER, "papel")
    @Test fun nightEdition() = tour(ThemeMode.NIGHT, "noturno")

    /** Same walk at real-time pace (no captures) — run it under `adb shell screenrecord` to review motion. */
    @Test fun motionReel() = tour(ThemeMode.PAPER, "rolo", realTime = true)

    private var realTime = false

    private fun tour(mode: ThemeMode, edition: String, realTime: Boolean = false) {
        this.realTime = realTime
        val retrofit = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resources = retrofit.create(PreloResourceApi::class.java)
        compose.runOnUiThread {
            compose.activity.setContent {
                WorkControlTheme(mode) {
                    SessionScreen(compose.activity, TourSession(), retrofit.create(PreloApi::class.java), resources,
                        DevicePreferences(context), ApprovalDecisions(resources), TourEvents(), PushRouting(), TourPush(),
                        DeviceSecurity { true }, themeMode = mode)
                }
            }
        }
        compose.mainClock.autoAdvance = false
        val shots = File(context.getExternalFilesDir(null), "tour/$edition").apply { deleteRecursively(); mkdirs() }
        fun shot(name: String) {
            settle()
            if (realTime) return
            val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            File(shots, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        fun tap(text: String) { waitFor(text); compose.onAllNodesWithText(text).onFirst().performClick(); settle(400) }

        waitFor("Entre na redação"); shot("01-login")
        compose.onAllNodesWithText("E-mail").onFirst().performTextInput("ana@exotermo.com.br")
        compose.onAllNodesWithText("Senha").onFirst().performTextInput("senha-de-teste")
        tap("Entrar")
        waitFor("Confirme que é você"); shot("02-codigo")
        compose.onAllNodesWithText("Código").onFirst().performTextInput("123456")
        tap("Verificar")
        waitFor("Continuar de onde parou".uppercase()); shot("03-inicio")

        tap("Tarefas"); waitFor("Atualizar landing page da Loja Aurora"); shot("04-tarefas")
        tap("Atualizar landing page da Loja Aurora"); waitFor("▚ SAÍDA DO AGENTE"); shot("05-tarefa")

        tap("Aprovações"); waitFor("Send whatsapp"); shot("06-aprovacoes")
        tap("Send whatsapp"); waitFor("Aprovar"); tap("Aprovar"); waitFor("Confirmação reforçada".uppercase()); shot("07-confirmacao")

        tap("Deploys"); waitFor("Publicar a versão 2.4 da loja"); shot("08-deploys")
        tap("Mais"); waitFor("Sala de máquinas"); shot("09-mais")
        tap("Sala de máquinas"); waitFor("vps-loja-01"); shot("10-maquinas")
        tap("Mais"); tap("Linha de montagem"); waitFor("Revisar checkout"); shot("11-pipeline")
        tap("Mais"); tap("Arquivo"); waitFor("contrato-aurora.pdf"); shot("12-arquivos")
        tap("Mais"); tap("Sua conta"); waitFor("Aparelhos conectados".uppercase()); shot("13-conta")
        tap("Início"); waitFor("Continuar de onde parou".uppercase()); tap("＋ Nova tarefa"); waitFor("O que o agente deve fazer?"); shot("14-nova-tarefa")
    }

    /** Advances the (manual) Compose clock so springs settle and network results recompose. */
    private fun settle(ms: Long = 2_600) {
        var left = ms
        val step = if (realTime) 16L else 100L
        while (left > 0) { compose.mainClock.advanceTimeBy(step); Thread.sleep(if (realTime) 16 else 15); left -= step }
    }

    private fun waitFor(text: String, timeoutMs: Long = 10_000) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (compose.onAllNodes(hasText(text, substring = false), useUnmergedTree = true).nodes().isNotEmpty()) return
            if (realTime) { compose.mainClock.advanceTimeBy(16); Thread.sleep(16) } else { compose.mainClock.advanceTimeBy(100); Thread.sleep(30) }
        }
        throw AssertionError("Não apareceu na tela: $text")
    }

    private fun SemanticsNodeInteractionCollection.nodes() = runCatching { fetchSemanticsNodes() }.getOrDefault(emptyList())
}

private object TourData : Dispatcher() {
    private fun ago(minutes: Long) = OffsetDateTime.now().minusMinutes(minutes).toString()
    private fun ahead(minutes: Long) = OffsetDateTime.now().plusMinutes(minutes).toString()

    override fun dispatch(request: RecordedRequest): MockResponse {
        val path = request.path?.substringBefore('?') ?: ""
        if (request.method == "POST" && path.endsWith("/approve")) {
            return json("""{"code":"step_up_required","message":"confirm with TOTP"}""", 403)
        }
        val body = when {
            path == "/api/v1/me" -> """{"userId":"u1","email":"ana@exotermo.com.br","role":"ADMIN","scopes":[],"workspaceId":"w1","workspaceName":"Exotermo","projects":[{"id":"p1","name":"Loja Aurora","clientId":null},{"id":"p2","name":"Clínica Sol","clientId":null}],"session":{"kind":"mobile","deviceId":"d1","deviceName":"Pixel"}}"""
            path == "/api/v1/projects" -> """[{"id":"p1","name":"Loja Aurora"},{"id":"p2","name":"Clínica Sol"}]"""
            path == "/api/v1/home" -> """{"recent":[
                {"kind":"TASK","id":"t1","title":"Atualizar landing page da Loja Aurora","subtitle":"Loja Aurora","status":"RUNNING","projectId":"p1","viewedAt":"${ago(4)}"},
                {"kind":"PROJECT","id":"p2","title":"Clínica Sol","subtitle":"3 tarefas abertas","status":"","projectId":"p2","viewedAt":"${ago(70)}"},
                {"kind":"CLIENT","id":"c1","title":"Padaria Bom Dia","subtitle":"prospecção","status":"","projectId":null,"viewedAt":"${ago(1500)}"}],
              "pending":[
                {"kind":"APPROVAL","id":"a1","taskId":"t2","title":"Enviar WhatsApp para 3 clientes","detail":"risco alto","projectId":"p1","projectName":"Loja Aurora","at":"${ago(2)}"},
                {"kind":"RUNNING","id":"t1","taskId":"t1","title":"Atualizar landing page da Loja Aurora","detail":"agente coder","projectId":"p1","projectName":"Loja Aurora","at":"${ago(4)}"},
                {"kind":"FAILED","id":"t3","taskId":"t3","title":"Gerar relatório semanal","detail":"tempo esgotado","projectId":"p2","projectName":"Clínica Sol","at":"${ago(180)}"}]}"""
            path == "/api/v1/agents" -> """[{"id":"coder","name":"Coder","description":""},{"id":"seller","name":"Prospecção","description":""},{"id":"ops","name":"Operações","description":""}]"""
            path == "/api/v1/tasks" -> """[
                {"id":"t1","description":"Atualizar landing page da Loja Aurora","status":"RUNNING","agentId":"coder","createdAt":"${ago(4)}","source":"MANUAL","projectId":"p1"},
                {"id":"t2","description":"Avisar clientes sobre a promoção de outubro","status":"WAITING_APPROVAL","agentId":"seller","createdAt":"${ago(9)}","source":"MESSAGING","projectId":"p1"},
                {"id":"t4","description":"Conferir certificado TLS do domínio","status":"COMPLETED","agentId":"ops","createdAt":"${ago(240)}","source":"MANUAL","projectId":"p1"},
                {"id":"t3","description":"Gerar relatório semanal","status":"FAILED","agentId":"ops","createdAt":"${ago(180)}","source":"MANUAL","projectId":"p1"}]"""
            path == "/api/v1/tasks/t1" -> """{"id":"t1","description":"Atualizar landing page da Loja Aurora","status":"RUNNING","agentId":"coder","createdAt":"${ago(4)}","source":"MANUAL","projectId":"p1"}"""
            path == "/api/v1/tasks/t1/tree" -> """{"taskId":"t1","description":"Atualizar landing page","status":"RUNNING","agentId":"coder","depth":0,"children":[
                {"taskId":"t5","description":"Revisar textos com o cliente","status":"COMPLETED","agentId":"seller","depth":1,"children":[]},
                {"taskId":"t6","description":"Publicar em staging","status":"QUEUED","agentId":"ops","depth":1,"children":[]}]}"""
            path == "/api/v1/tasks/t1/executions/latest" || path == "/api/v1/tasks/t1/executions/e1" ->
                """{"executionId":"e1","taskId":"t1","agentId":"coder","status":"RUNNING","result":null,"error":null,"model":"claude-sonnet","provider":"cli"}"""
            path == "/api/v1/tasks/t1/executions/e1/turns" -> """[
                {"turnNumber":1,"kind":"LLM_CALL","output":"Vou ler a página atual e propor as mudanças de texto.","startedAt":"${ago(4)}"},
                {"turnNumber":2,"kind":"TOOL_CALL","output":"workspace.read index.html → 18 KB","startedAt":"${ago(3)}"},
                {"turnNumber":3,"kind":"SUBTASK","output":"Criada: Revisar textos com o cliente","startedAt":"${ago(2)}"}]"""
            path == "/api/v1/approvals" -> """[
                {"id":"a1","toolCallId":"tc1","scope":"tool:send_whatsapp","status":"PENDING","requestedAt":"${ago(2)}","expiresAt":"${ahead(26)}","shortCode":"KQTZ"},
                {"id":"a2","scope":"action:deploy","status":"PENDING","requestedAt":"${ago(1)}","expiresAt":"${ahead(29)}","shortCode":"MXRA","actionRequestId":"ar1"},
                {"id":"a3","toolCallId":"tc2","scope":"tool:server_exec","status":"APPROVED","requestedAt":"${ago(300)}","expiresAt":"${ago(270)}","decidedAt":"${ago(290)}","decidedBy":"whatsapp:+55…"}]"""
            path == "/api/v1/approvals/a1" -> """{"id":"a1","toolCallId":"tc1","scope":"tool:send_whatsapp","status":"PENDING","requestedAt":"${ago(2)}","expiresAt":"${ahead(26)}","shortCode":"KQTZ"}"""
            path.startsWith("/api/v1/projects/") && path.endsWith("/actions") -> """[
                {"id":"ar1","kind":"deploy","impact":"Publicar a versão 2.4 da loja","status":"PENDING","payload":{"repository":"exotermo/loja-aurora","commitSha":"9f2c4e1a7b3d5f60718293a4b5c6d7e8f9012345","environment":"production","target":"loja.aurora.com.br","app":"loja","deployRequestId":"d9"},"result":null,"createdAt":"${ago(1)}"},
                {"id":"ar0","kind":"deploy","impact":"Corrigir cálculo de frete","status":"APPROVED","payload":{"repository":"exotermo/loja-aurora","commitSha":"0123456789abcdef0123456789abcdef01234567","environment":"staging","target":"staging.aurora.com.br","app":"loja","deployRequestId":"d8"},"result":{"status":"SUCCEEDED","sequence":3,"message":"Saudável em 41 s","url":"https://staging.aurora.com.br","reportedAt":"${ago(95)}"},"createdAt":"${ago(120)}"}]"""
            path == "/api/v1/servers" -> """[
                {"id":"s1","name":"vps-loja-01","host":"203.0.113.10","sshPort":22,"sshUser":"deploy","lastStatus":"ONLINE","lastCheckedAt":"${ago(6)}"},
                {"id":"s2","name":"worker-kvm","host":"10.0.0.21","sshPort":22,"sshUser":"bastion","lastStatus":"OFFLINE","lastCheckedAt":"${ago(50)}","lastError":"connection timed out"}]"""
            path == "/api/v1/pipeline" -> """[{"taskId":"t1","description":"Atualizar landing page","agentId":"coder","depth":0,"pipelineStatus":"AWAITING_SUBTASK","children":[
                {"taskId":"t5","description":"Revisar checkout","agentId":"seller","depth":1,"pipelineStatus":"AWAITING_APPROVAL","children":[]},
                {"taskId":"t6","description":"Publicar em staging","agentId":"ops","depth":1,"pipelineStatus":"QUEUED","children":[]}]}]"""
            path.endsWith("/files") -> """{"files":[
                {"id":"f1","name":"contrato-aurora.pdf","contentType":"application/pdf","kind":"pdf","sizeBytes":284311,"createdAt":"${ago(2000)}"},
                {"id":"f2","name":"logo-aurora.png","contentType":"image/png","kind":"image","sizeBytes":48211,"createdAt":"${ago(4000)}"}],"totalBytes":332522,"maxFileBytes":104857600}"""
            path == "/api/v1/me/sessions" -> """[{"id":"m1","deviceName":"Pixel 8","platform":"android","createdAt":"${ago(60)}","lastUsedAt":"${ago(1)}","expiresAt":"${ahead(40000)}","current":true}]"""
            else -> "[]"
        }
        return json(body)
    }

    private fun json(body: String, code: Int = 200) =
        MockResponse().setResponseCode(code).addHeader("Content-Type", "application/json").setBody(body)
}

private class TourSession : com.workcontrol.app.data.auth.SessionController {
    private val state = kotlinx.coroutines.flow.MutableStateFlow<com.workcontrol.app.data.auth.SessionPhase>(
        com.workcontrol.app.data.auth.SessionPhase.Login())
    override val phase: kotlinx.coroutines.flow.StateFlow<com.workcontrol.app.data.auth.SessionPhase> = state
    override suspend fun start() = Unit
    override suspend fun login(email: String, password: String) { state.value = com.workcontrol.app.data.auth.SessionPhase.Code }
    override suspend fun verify(activity: androidx.fragment.app.FragmentActivity, code: String): Boolean {
        state.value = com.workcontrol.app.data.auth.SessionPhase.Ready; return true
    }
    override suspend fun unlockAndRestore(activity: androidx.fragment.app.FragmentActivity) = true
    override suspend fun logout(activity: androidx.fragment.app.FragmentActivity) = true
    override fun sessionEnded() = Unit
}

private class TourEvents : com.workcontrol.app.data.prelo.EventFeed {
    override val connected = kotlinx.coroutines.flow.MutableStateFlow(true)
    override val events = kotlinx.coroutines.flow.MutableSharedFlow<com.workcontrol.app.data.prelo.PreloEvent>()
    override fun start() = Unit
    override fun stop() = Unit
}

private class TourPush : com.workcontrol.app.data.push.PushControl {
    override val serverEnabled = kotlinx.coroutines.flow.MutableStateFlow(true)
    override suspend fun onLogin() = Unit
    override suspend fun setWanted(value: Boolean) = true
}
