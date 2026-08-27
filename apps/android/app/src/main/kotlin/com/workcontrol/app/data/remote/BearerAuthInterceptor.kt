package com.workcontrol.app.data.remote

import com.workcontrol.app.data.auth.AccessTokenProvider
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

class BearerAuthInterceptor @Inject constructor(
    private val accessTokenProvider: AccessTokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val accessToken = accessTokenProvider.accessToken()?.takeIf(String::isNotBlank)
        val authenticated = if (accessToken == null || original.header("Authorization") != null) {
            original
        } else {
            original.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        }
        return chain.proceed(authenticated)
    }
}
