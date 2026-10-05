package com.workcontrol.app.data.prelo

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreloEventsTest {
    @Test fun parsesSseBlocksCommentsAndRetryWithoutTreatingCommentsAsEvents() {
        val server = MockWebServer()
        server.start()
        val frame = """: ping
retry: 5000

event: task
data: {"kind":"task","id":"t1","projectId":"p1","status":"RUNNING"}

"""
        server.enqueue(MockResponse().setHeader("Content-Type", "text/event-stream").setBody(frame))
        val latch = CountDownLatch(1)
        val received = mutableListOf<PreloEvent>()
        val source = EventSources.createFactory(OkHttpClient()).newEventSource(
            Request.Builder().url(server.url("/api/v1/events/stream"))
                .header("Authorization", "Bearer example").build(),
            object : EventSourceListener() {
                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    parsePreloEvent(type, data)?.let { received.add(it) }
                    latch.countDown()
                }
            },
        )
        try {
            assertTrue(latch.await(3, TimeUnit.SECONDS))
            assertEquals(1, received.size)
            assertEquals("task", received.single().kind)
            assertEquals("t1", received.single().id)
            assertEquals("p1", received.single().projectId)
            assertEquals("Bearer example", server.takeRequest().getHeader("Authorization"))
        } finally { source.cancel(); server.shutdown() }
    }

    @Test fun malformedEventDoesNotRefreshAnyView() {
        assertEquals(null, parsePreloEvent("task", "not json"))
    }
}
