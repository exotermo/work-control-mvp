package com.workcontrol.app.data.auth

import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class PreloAuthenticator @Inject constructor(private val session: AuthSession) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) { count++; prior = prior.priorResponse }
        if (count > 1) return null
        val used = response.request.header("Authorization")?.removePrefix("Bearer ")
        val replacement = runBlocking { session.refreshIfNeeded(used) } ?: return null
        return response.request.newBuilder().header("Authorization", "Bearer $replacement").build()
    }
}
