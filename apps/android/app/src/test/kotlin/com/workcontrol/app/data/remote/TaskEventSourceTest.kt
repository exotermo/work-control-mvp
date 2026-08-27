package com.workcontrol.app.data.remote

import com.workcontrol.app.data.auth.InMemoryAccessTokenStore
import com.workcontrol.app.domain.error.WorkControlException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskEventSourceTest {
    private val server = MockWebServer()

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `websocket envia bearer em header e recebe envelope`() = runBlocking {
        val source = sourceWithToken("ws-token")
        server.enqueue(webSocketResponse("task_updated"))

        val event = withTimeout(5_000) { source.observe(TASK_ID).first() }

        assertEquals("task_updated", event.type)
        val request = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertEquals("Bearer ws-token", request.getHeader("Authorization"))
        assertEquals("/v1/tasks/$TASK_ID/events/ws", request.path)
    }

    @Test
    fun `reconecta depois de falha transitoria`() = runBlocking {
        val source = sourceWithToken("ws-token")
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(webSocketResponse("agent.progress"))

        val event = withTimeout(5_000) { source.observe(TASK_ID).first() }

        assertEquals("agent.progress", event.type)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `rejeita evento de outra tarefa como falha de protocolo`() = runBlocking {
        val source = sourceWithToken("ws-token")
        server.enqueue(
            MockResponse().withWebSocketUpgrade(
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        webSocket.send("""{"type":"task_updated","task_id":"outra-tarefa"}""")
                    }
                },
            ),
        )

        val failure = runCatching {
            withTimeout(5_000) { source.observe(TASK_ID).first() }
        }.exceptionOrNull()

        assertTrue(failure is WorkControlException.Protocol)
    }

    private fun sourceWithToken(token: String): TaskEventSource {
        val tokenStore = InMemoryAccessTokenStore().apply { replace(token) }
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerAuthInterceptor(tokenStore))
            .build()
        return TaskEventSource(
            client = client,
            apiBaseUrl = server.url("/"),
            json = Json { ignoreUnknownKeys = true },
            reconnectDelayMillis = { 0L },
        )
    }

    private fun webSocketResponse(type: String): MockResponse = MockResponse().withWebSocketUpgrade(
        object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("""{"type":"$type","task_id":"$TASK_ID","ignored":true}""")
            }
        },
    )

    private companion object {
        const val TASK_ID = "00000000-0000-0000-0000-000000000184"
    }
}
