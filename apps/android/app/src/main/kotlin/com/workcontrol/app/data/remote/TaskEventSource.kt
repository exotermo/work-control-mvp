package com.workcontrol.app.data.remote

import com.workcontrol.app.domain.error.WorkControlException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

@Serializable
data class TaskEventEnvelope(
    val type: String,
    @SerialName("task_id") val taskId: String,
)

@Singleton
class TaskEventSource internal constructor(
    private val client: OkHttpClient,
    @ApiBaseUrl private val apiBaseUrl: HttpUrl,
    private val json: Json,
    private val reconnectDelayMillis: (failedAttempts: Int) -> Long,
) {
    @Inject
    constructor(
        client: OkHttpClient,
        @ApiBaseUrl apiBaseUrl: HttpUrl,
        json: Json,
    ) : this(
        client = client,
        apiBaseUrl = apiBaseUrl,
        json = json,
        reconnectDelayMillis = { attempts ->
            min(30, 1 shl min(attempts, 5)) * 1_000L
        },
    )

    fun observe(taskId: String): Flow<TaskEventEnvelope> = channelFlow {
        var failedAttempts = 0
        while (currentCoroutineContext().isActive) {
            val disconnected = CompletableDeferred<Unit>()
            val opened = AtomicBoolean(false)
            val request = Request.Builder()
                .url(apiBaseUrl.taskEventsUrl(taskId))
                .build()
            val socket = client.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        opened.set(true)
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        runCatching { json.decodeFromString<TaskEventEnvelope>(text) }
                            .onSuccess { event ->
                                if (event.taskId == taskId) {
                                    trySend(event)
                                } else {
                                    disconnected.completeExceptionally(WorkControlException.Protocol())
                                }
                            }
                            .onFailure {
                                disconnected.completeExceptionally(WorkControlException.Protocol(it))
                            }
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        disconnected.complete(Unit)
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        val terminalFailure = when (response?.code) {
                            401 -> WorkControlException.Authentication(t)
                            403 -> WorkControlException.Authorization(t)
                            in 400..499 -> WorkControlException.Protocol(t)
                            else -> null
                        }
                        response?.close()
                        if (terminalFailure == null) {
                            disconnected.complete(Unit)
                        } else {
                            disconnected.completeExceptionally(terminalFailure)
                        }
                    }
                },
            )
            try {
                disconnected.await()
            } finally {
                socket.cancel()
            }
            if (!currentCoroutineContext().isActive) break

            failedAttempts = if (opened.get()) 0 else failedAttempts + 1
            delay(reconnectDelayMillis(failedAttempts))
        }
    }
}

private fun HttpUrl.taskEventsUrl(taskId: String): HttpUrl = newBuilder()
    .addPathSegments("v1/tasks")
    .addPathSegment(taskId)
    .addPathSegments("events/ws")
    .build()
