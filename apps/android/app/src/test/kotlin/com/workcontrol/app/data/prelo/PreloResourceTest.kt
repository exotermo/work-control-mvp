package com.workcontrol.app.data.prelo

import com.google.gson.Gson
import com.workcontrol.app.data.auth.DevicePreferences
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PreloResourceTest {
    @Test fun listsFilesAndDownloadsExactProjectContent() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().addHeader("Content-Type", "application/json")
                .setBody("""{"files":[{"id":"f1","name":"brief.pdf"}],"totalBytes":3,"maxFileBytes":1000}"""))
            server.enqueue(MockResponse().addHeader("Content-Type", "application/pdf").setBody("pdf"))
            val api = Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(PreloResourceApi::class.java)
            assertEquals("f1", api.files("p1").asJsonObject.getAsJsonArray("files")[0].asJsonObject.get("id").asString)
            assertEquals("pdf", api.fileContent("p1", "f1").use { it.string() })
            assertEquals("/api/v1/projects/p1/files", server.takeRequest().path)
            assertEquals("/api/v1/projects/p1/files/f1/content", server.takeRequest().path)
        } finally { server.shutdown() }
    }

    @Test fun projectHeaderIsScopedToProjectResources() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("[]").addHeader("Content-Type", "application/json"))
            server.enqueue(MockResponse().setBody("[]").addHeader("Content-Type", "application/json"))
            val preferences = mockk<DevicePreferences>()
            coEvery { preferences.projectId() } returns "project-1"
            val client = OkHttpClient.Builder().addInterceptor(ProjectHeaderInterceptor(preferences)).build()
            val api = Retrofit.Builder().baseUrl(server.url("/"))
                .client(client).addConverterFactory(GsonConverterFactory.create(Gson())).build()
                .create(PreloResourceApi::class.java)
            api.tasks()
            api.projects()
            assertEquals("project-1", server.takeRequest().getHeader("X-Project-Id"))
            assertEquals(null, server.takeRequest().getHeader("X-Project-Id"))
        } finally { server.shutdown() }
    }

    @Test fun approvalStepUpRequiresTotpAndDenialDoesNot() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(403).addHeader("Content-Type", "application/json")
                .setBody("""{"code":"step_up_required"}"""))
            server.enqueue(MockResponse().addHeader("Content-Type", "application/json").setBody("{}"))
            server.enqueue(MockResponse().addHeader("Content-Type", "application/json").setBody("{}"))
            val api = Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(PreloResourceApi::class.java)
            val decisions = ApprovalDecisions(api)
            assertEquals(ApproveOutcome.TotpRequired, decisions.approve("a1"))
            assertEquals(ApproveOutcome.Done, decisions.approve("a1", "123456"))
            decisions.deny("a2")
            val first = server.takeRequest()
            val second = server.takeRequest()
            val third = server.takeRequest()
            assertEquals("/api/v1/approvals/a1/approve", first.path)
            assertFalse(first.body.readUtf8().contains("totpCode"))
            assertEquals("123456", Gson().fromJson(second.body.readUtf8(), ApprovalDecision::class.java).totpCode)
            assertEquals("/api/v1/approvals/a2/deny", third.path)
            assertFalse(third.body.readUtf8().contains("totpCode"))
        } finally { server.shutdown() }
    }

    @Test fun forbiddenResourceIsNotRetriedAsApproval() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(403).addHeader("Content-Type", "application/json")
                .setBody("""{"code":"forbidden"}"""))
            val api = Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(PreloResourceApi::class.java)
            try { ApprovalDecisions(api).approve("a1"); throw AssertionError("expected 403") }
            catch (failure: HttpException) { assertEquals(403, failure.code()) }
            assertEquals(1, server.requestCount)
        } finally { server.shutdown() }
    }
}
