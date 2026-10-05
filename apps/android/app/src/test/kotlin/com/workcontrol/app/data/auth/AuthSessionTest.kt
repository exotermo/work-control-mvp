package com.workcontrol.app.data.auth

import com.google.gson.Gson
import com.workcontrol.app.data.prelo.MobileTokens
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AuthSessionTest {
    private val api = mockk<PreloAuthApi>()
    private val store = mockk<SecureRefreshStore>()
    private val device = mockk<DevicePreferences>()
    private val unlock = mockk<LocalUnlock>()
    private val access = InMemoryAccessTokenStore()
    private val session = AuthSession(api, store, device, access, unlock, Gson())

    private fun setup() {
        access.replace("old-access")
        every { store.read() } returns "old-refresh"
        every { store.write(any()) } just Runs
        every { store.clear() } just Runs
        coEvery { device.deviceId() } returns "device-id"
    }

    @Test fun concurrent401RotatesOnlyOnce() = runTest {
        setup()
        coEvery { api.refresh(any()) } coAnswers {
            delay(50)
            MobileTokens("new-access", 900, "new-refresh", "later", "session-id")
        }
        val results = (1..12).map { async { session.refreshIfNeeded("old-access") } }.awaitAll()
        assertTrue(results.all { it == "new-access" })
        coVerify(exactly = 1) { api.refresh(any()) }
        verify(exactly = 1) { store.write("new-refresh") }
        assertEquals("new-access", access.accessToken())
    }

    @Test fun reusedRefreshEndsTheLocalSession() = runTest {
        setup()
        val body = """{"code":"refresh_reused"}""".toResponseBody("application/json".toMediaType())
        coEvery { api.refresh(any()) } throws HttpException(Response.error<Any>(401, body))
        assertNull(session.refreshIfNeeded("old-access"))
        verify(exactly = 1) { store.clear() }
        assertNull(access.accessToken())
        assertEquals("Sua sessão foi encerrada por segurança.", (session.phase.value as SessionPhase.Login).notice)
    }

    @Test fun failedDurableRotationCannotReuseOldRefresh() = runTest {
        setup()
        coEvery { api.refresh(any()) } returns MobileTokens("new-access", 900, "new-refresh", "later", "session-id")
        every { store.write("new-refresh") } throws IllegalStateException("storage failure")
        assertNull(session.refreshIfNeeded("old-access"))
        verify { store.clear() }
        assertNull(access.accessToken())
    }
}
