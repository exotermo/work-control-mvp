package com.workcontrol.app.data.prelo

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.workcontrol.app.data.auth.AuthSession
import com.workcontrol.app.data.auth.InMemoryAccessTokenStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import kotlin.coroutines.resume

data class PreloEvent(val kind: String, val id: String?, val projectId: String?,
    val taskId: String?, val parentId: String?, val status: String?)

fun parsePreloEvent(type: String?, data: String): PreloEvent? = runCatching {
    val json = JsonParser.parseString(data).asJsonObject
    fun value(name: String): String? = json.get(name)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString
    val kind = value("kind") ?: type ?: return@runCatching null
    PreloEvent(kind, value("id"), value("projectId"), value("taskId"), value("parentId"), value("status"))
}.getOrNull()

/** Foreground-only notification stream. Every event triggers a fresh authorized API read. */
@Singleton
class PreloEvents @Inject constructor(
    client: OkHttpClient,
    private val access: InMemoryAccessTokenStore,
    private val session: AuthSession,
    @com.workcontrol.app.data.remote.ApiBaseUrl private val baseUrl: okhttp3.HttpUrl,
) {
    private val factory = EventSources.createFactory(client.newBuilder().readTimeout(0, java.util.concurrent.TimeUnit.SECONDS).build())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private val mutableConnected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = mutableConnected
    private val mutableEvents = MutableSharedFlow<PreloEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<PreloEvent> = mutableEvents

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            var backoff = 1000L
            while (isActive && session.phase.value == com.workcontrol.app.data.auth.SessionPhase.Ready) {
                val token = access.accessToken() ?: break
                val request = Request.Builder().url(baseUrl.newBuilder().addPathSegments("api/v1/events/stream").build())
                    .header("Authorization", "Bearer $token").build()
                val status = suspendCancellableCoroutine<Int> { continuation ->
                    val source = factory.newEventSource(request, object : EventSourceListener() {
                        override fun onOpen(eventSource: EventSource, response: Response) {
                            mutableConnected.value = true
                        }
                        override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                            val event = parsePreloEvent(type, data) ?: return
                            if (event.kind == "session_ended") {
                                session.sessionEnded()
                                eventSource.cancel()
                            } else mutableEvents.tryEmit(event)
                        }
                        override fun onClosed(eventSource: EventSource) {
                            mutableConnected.value = false
                            if (continuation.isActive) continuation.resume(0)
                        }
                        override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                            mutableConnected.value = false
                            if (continuation.isActive) continuation.resume(response?.code ?: 0)
                        }
                    })
                    continuation.invokeOnCancellation { source.cancel(); mutableConnected.value = false }
                }
                if (status == 429 || status == 403 || session.phase.value != com.workcontrol.app.data.auth.SessionPhase.Ready) break
                if (status == 401) {
                    val replacement = session.refreshIfNeeded(token)
                    if (replacement == null) break
                }
                delay(backoff)
                backoff = (backoff * 2).coerceAtMost(30_000L)
            }
            mutableConnected.value = false
        }
    }

    fun stop() { job?.cancel(); job = null; mutableConnected.value = false }
}
