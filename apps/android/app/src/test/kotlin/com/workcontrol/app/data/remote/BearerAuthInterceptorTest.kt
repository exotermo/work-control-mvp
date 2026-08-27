package com.workcontrol.app.data.remote

import com.workcontrol.app.data.auth.InMemoryAccessTokenStore
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BearerAuthInterceptorTest {
    private val server = MockWebServer()

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `adiciona bearer sem colocar token na URL`() {
        val tokens = InMemoryAccessTokenStore().apply { replace("access-token-for-test") }
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerAuthInterceptor(tokens))
            .build()
        server.enqueue(MockResponse().setResponseCode(204))

        client.newCall(Request.Builder().url(server.url("/v1/me")).build()).execute().use { response ->
            assertEquals(204, response.code)
        }

        val request = server.takeRequest()
        assertEquals("Bearer access-token-for-test", request.getHeader("Authorization"))
        assertNull(request.requestUrl?.query)
    }

    @Test
    fun `nao inventa header quando ainda nao existe sessao`() {
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerAuthInterceptor(InMemoryAccessTokenStore()))
            .build()
        server.enqueue(MockResponse().setResponseCode(204))

        client.newCall(Request.Builder().url(server.url("/health/live")).build()).execute().close()

        assertNull(server.takeRequest().getHeader("Authorization"))
    }
}
