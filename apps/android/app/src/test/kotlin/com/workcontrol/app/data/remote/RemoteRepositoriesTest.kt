package com.workcontrol.app.data.remote

import com.google.gson.Gson
import com.workcontrol.app.domain.model.AgentRuntimeStatus
import com.workcontrol.app.domain.model.DeviceStatus
import com.workcontrol.app.domain.model.TaskStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RemoteRepositoriesTest {
    private val server = MockWebServer()
    private val gson = Gson()
    private val client = OkHttpClient()
    private val api by lazy {
        Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(WorkControlApi::class.java)
    }
    private val executor = ApiCallExecutor(gson)

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `workspace tarefas agentes maquinas e approval usam o contrato Go`() = runTest {
        enqueueJson(
            """{"user":{"id":"u1","email":"dev@local","display_name":"Dev"},"workspaces":[{"id":"w1","name":"Escritório","slug":"escritorio","role":"owner"}]}""",
        )
        enqueueJson(
            """[{"id":"t1","code":"#184","title":"Corrigir bug","status":"IN_PROGRESS","active_agent_count":3,"progress_percent":68}]""",
        )
        enqueueJson(
            """[{"id":"a1","name":"Agent Dev","role":"DEV","model":"model","machine_name":"PC","status":"RUNNING","progress_percent":68,"current_activity":"Patch"}]""",
        )
        enqueueJson(
            """[{"id":"d1","name":"PC","role_label":"Dev Machine","status":"ONLINE","cpu_percent":45,"ram_percent":62,"disk_percent":38,"active_agent_count":1}]""",
        )
        enqueueJson(
            """[{"id":"p1","title":"Deploy","requested_by":"Agent DevOps","description":"Staging","payload":{"environment":"staging","branch":"main","commit_summary":"1 commit","image":"app:1","rollback_policy":"auto","tests_passed":4,"tests_total":4},"decision":null}]""",
        )

        val events = TaskEventSource(client, server.url("/"), Json { ignoreUnknownKeys = true }) { 0L }
        val workspace = RemoteWorkspaceRepository(api, executor).observeCurrentWorkspace().first()
        val tasks = RemoteTaskRepository(api, executor, events).observeTasks().first()
        val agents = RemoteAgentRepository(api, executor).observeActiveAgents().first()
        val machines = RemoteMachineRepository(api, executor).observeMachines().first()
        val approval = RemoteApprovalRepository(api, executor).observePendingApproval().first()

        assertEquals("Escritório", workspace.name)
        assertEquals(TaskStatus.IN_PROGRESS, tasks.single().status)
        assertEquals(AgentRuntimeStatus.RUNNING, agents.single().status)
        assertEquals(DeviceStatus.ONLINE, machines.single().status)
        assertEquals("staging", approval?.environment)
        assertNull(approval?.decision)
        assertEquals("/v1/me", server.takeRequest().path)
        assertEquals("/v1/tasks", server.takeRequest().path)
        assertEquals("/v1/agents", server.takeRequest().path)
        assertEquals("/v1/devices", server.takeRequest().path)
        assertEquals("/v1/approvals", server.takeRequest().path)
    }

    private fun enqueueJson(body: String) {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body),
        )
    }
}
