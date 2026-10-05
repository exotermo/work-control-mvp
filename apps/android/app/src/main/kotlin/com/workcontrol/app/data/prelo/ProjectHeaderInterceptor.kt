package com.workcontrol.app.data.prelo

import com.workcontrol.app.data.auth.DevicePreferences
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/** Adds project context only to endpoints whose contract consumes X-Project-Id. */
class ProjectHeaderInterceptor @Inject constructor(private val preferences: DevicePreferences) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val scoped = path == "/api/v1/home" || path == "/api/v1/pipeline" ||
            path.startsWith("/api/v1/tasks") || path.startsWith("/api/v1/approvals") ||
            path.startsWith("/api/v1/servers")
        val selected = if (scoped) runBlocking { preferences.projectId() } else null
        val next = if (selected == null) request else request.newBuilder().header("X-Project-Id", selected).build()
        return chain.proceed(next)
    }
}
