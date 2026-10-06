package com.workcontrol.app.data.prelo

import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import okhttp3.ResponseBody

/** Read models stay as server JSON; the server owns state and authorization. */
interface PreloResourceApi {
    @PUT("api/v1/me/push-token") suspend fun registerPush(@Body body: PushToken): PushRegistration
    @DELETE("api/v1/me/push-token") suspend fun deletePush()
    @GET("api/v1/home") suspend fun home(): JsonElement
    @GET("api/v1/projects") suspend fun projects(): JsonElement
    @GET("api/v1/clients") suspend fun clients(): JsonElement
    @GET("api/v1/clients/{id}") suspend fun client(@Path("id") id: String): JsonElement
    @GET("api/v1/clients/{id}/timeline")
    suspend fun clientTimeline(@Path("id") id: String, @Query("before") before: String? = null): JsonElement
    @POST("api/v1/recent") suspend fun recent(@Body body: RecentTouch): JsonElement
    @GET("api/v1/agents") suspend fun agents(): JsonElement
    @GET("api/v1/tasks") suspend fun tasks(): JsonElement
    @GET("api/v1/tasks/{id}") suspend fun task(@Path("id") id: String): JsonElement
    @GET("api/v1/tasks/{id}/tree") suspend fun tree(@Path("id") id: String): JsonElement
    @GET("api/v1/tasks/{id}/executions/latest") suspend fun latest(@Path("id") id: String): JsonElement
    @GET("api/v1/tasks/{id}/executions/{executionId}")
    suspend fun execution(@Path("id") id: String, @Path("executionId") executionId: String): JsonElement
    @GET("api/v1/tasks/{id}/executions/{executionId}/turns")
    suspend fun turns(@Path("id") id: String, @Path("executionId") executionId: String): JsonElement
    @POST("api/v1/tasks") suspend fun createTask(@Body body: CreateTask): JsonElement
    @POST("api/v1/tasks/{id}/execute") suspend fun execute(@Path("id") id: String): JsonElement
    @GET("api/v1/approvals") suspend fun approvals(): JsonElement
    @GET("api/v1/approvals/{id}") suspend fun approval(@Path("id") id: String): JsonElement
    @POST("api/v1/approvals/{id}/approve")
    suspend fun approve(@Path("id") id: String, @Body body: ApprovalDecision): JsonElement
    @POST("api/v1/approvals/{id}/deny")
    suspend fun deny(@Path("id") id: String, @Body body: ApprovalDecision = ApprovalDecision()): JsonElement
    @GET("api/v1/servers") suspend fun servers(): JsonElement
    @GET("api/v1/servers/{id}") suspend fun server(@Path("id") id: String): JsonElement
    @POST("api/v1/servers/{id}/health-check") suspend fun healthCheck(@Path("id") id: String): JsonElement
    @GET("api/v1/pipeline") suspend fun pipeline(): JsonElement
    @GET("api/v1/projects/{projectId}/actions")
    suspend fun deploys(@Path("projectId") projectId: String, @Query("kind") kind: String = "deploy",
        @Query("limit") limit: Int = 50): JsonElement
    @GET("api/v1/projects/{projectId}/files") suspend fun files(@Path("projectId") projectId: String): JsonElement
    @Streaming
    @GET("api/v1/projects/{projectId}/files/{fileId}/content")
    suspend fun fileContent(@Path("projectId") projectId: String, @Path("fileId") fileId: String): ResponseBody
}

data class ApprovalDecision(val totpCode: String? = null)
data class PushToken(val token: String)
data class PushRegistration(val registered: Boolean, val pushEnabled: Boolean)
data class RecentTouch(val kind: String, val id: String)
