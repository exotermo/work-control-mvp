package com.workcontrol.app.data.auth

import com.workcontrol.app.data.prelo.LoginChallenge
import com.workcontrol.app.data.prelo.LoginRequest
import com.workcontrol.app.data.prelo.MobileRefreshRequest
import com.workcontrol.app.data.prelo.MobileTokens
import com.workcontrol.app.data.prelo.MobileVerifyRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.POST

/** Uses a bare OkHttp client: refresh must never recurse through the Authenticator. */
interface PreloAuthApi {
    @POST("api/v1/dashboard-auth/login")
    suspend fun login(@Body body: LoginRequest): LoginChallenge
    @POST("api/v1/dashboard-auth/mobile/verify")
    suspend fun verify(@Body body: MobileVerifyRequest): MobileTokens
    @POST("api/v1/dashboard-auth/mobile/refresh")
    suspend fun refresh(@Body body: MobileRefreshRequest): MobileTokens
    @POST("api/v1/dashboard-auth/mobile/logout")
    suspend fun logout(@Body body: MobileRefreshRequest)
    @DELETE("api/v1/me/push-token")
    suspend fun deletePushToken(@Header("Authorization") bearer: String)
}
