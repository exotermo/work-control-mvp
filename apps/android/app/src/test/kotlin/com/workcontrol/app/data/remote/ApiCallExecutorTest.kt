package com.workcontrol.app.data.remote

import com.google.gson.Gson
import com.workcontrol.app.domain.error.WorkControlException
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiCallExecutorTest {
    private val server = MockWebServer()
    private val gson = Gson()
    private val api by lazy {
        Retrofit.Builder()
            .baseUrl(server.url("/"))
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
    fun `mapeia 401 para sessao expirada sem expor corpo`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":"token interno que nao deve aparecer"}"""),
        )

        val failure = runCatching { executor.execute { api.me() } }.exceptionOrNull()

        assertTrue(failure is WorkControlException.Authentication)
        assertTrue(failure?.message.orEmpty().contains("token interno").not())
    }

    @Test
    fun `mapeia rate limit e preserva Retry-After tipado`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "12")
                .setBody("""{"error":"slow down"}"""),
        )

        val failure = runCatching { executor.execute { api.tasks() } }.exceptionOrNull()

        assertTrue(failure is WorkControlException.RateLimited)
        assertEquals(12L, (failure as WorkControlException.RateLimited).retryAfterSeconds)
    }
}
