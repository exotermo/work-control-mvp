package com.workcontrol.app.data.prelo

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Contract client. The Prelo server remains authoritative for identities,
// project membership, task state and approval decisions.
interface PreloApi {
    @POST("api/v1/dashboard-auth/login")
    suspend fun login(@Body body: LoginRequest): LoginChallenge

    @POST("api/v1/dashboard-auth/mobile/verify")
    suspend fun mobileVerify(@Body body: MobileVerifyRequest): MobileTokens

    @POST("api/v1/dashboard-auth/mobile/refresh")
    suspend fun mobileRefresh(@Body body: MobileRefreshRequest): MobileTokens

    @POST("api/v1/dashboard-auth/mobile/logout")
    suspend fun mobileLogout(@Body body: MobileRefreshRequest)

    @GET("api/v1/me")
    suspend fun me(): Me

    @GET("api/v1/me/sessions")
    suspend fun sessions(): List<MobileSession>

    @DELETE("api/v1/me/sessions/{id}")
    suspend fun revokeSession(@Path("id") id: String)

    @GET("api/v1/tasks")
    suspend fun tasks(@Header("X-Project-Id") projectId: String?): List<Task>

    @GET("api/v1/tasks/{id}")
    suspend fun task(@Path("id") id: String, @Header("X-Project-Id") projectId: String?): Task

    @POST("api/v1/tasks")
    suspend fun createTask(@Header("X-Project-Id") projectId: String?, @Body body: CreateTask): Task

    @POST("api/v1/tasks/{id}/execute")
    suspend fun execute(@Path("id") id: String, @Header("X-Project-Id") projectId: String?): Execution

    @GET("api/v1/pipeline")
    suspend fun pipeline(@Header("X-Project-Id") projectId: String?): List<PipelineNode>

    @GET("api/v1/servers")
    suspend fun servers(@Header("X-Project-Id") projectId: String?): List<Server>

    @GET("api/v1/approvals")
    suspend fun approvals(@Header("X-Project-Id") projectId: String?): List<Approval>

    @GET("api/v1/projects/{projectId}/actions")
    suspend fun deployActions(@Path("projectId") projectId: String, @Query("kind") kind: String = "deploy", @Query("limit") limit: Int = 50): List<ActionRequest>
}

data class LoginRequest(val email: String, val password: String)
data class LoginChallenge(val challenge: String, val nextStep: String)
data class MobileVerifyRequest(val challenge: String, val code: String, val deviceId: String,
    val deviceName: String, val platform: String = "android")
data class MobileRefreshRequest(val refreshToken: String, val deviceId: String)
data class MobileTokens(val accessToken: String, val expiresIn: Int, val refreshToken: String,
    val refreshExpiresAt: String, val sessionId: String? = null)
data class Me(val userId: String, val email: String, val role: String, val scopes: List<String>,
    val workspaceId: String, val workspaceName: String, val projects: List<Project>, val session: Session)
data class Session(val kind: String, val deviceId: String?, val deviceName: String?)
data class MobileSession(val id: String, val deviceName: String, val platform: String,
    val createdAt: String, val lastUsedAt: String, val expiresAt: String, val current: Boolean)
data class Project(val id: String, val name: String, val clientId: String?,
    val description: String? = null, val memberCount: Int? = null,
    val coverColor: String? = null, val defaultAgentId: String? = null)
data class Task(val id: String, val description: String, val status: String, val agentId: String,
    val createdAt: String, val projectId: String?)
data class CreateTask(val description: String, val agentId: String? = null)
data class Execution(val executionId: String, val taskId: String, val status: String,
    val result: String?, val error: String?)
data class PipelineNode(val taskId: String, val description: String, val agentId: String,
    val pipelineStatus: String, val children: List<PipelineNode>)
data class Server(val id: String, val projectId: String?, val name: String, val host: String,
    val lastStatus: String)
data class Approval(val id: String, val scope: String, val status: String, val expiresAt: String,
    val shortCode: String?)
data class ActionRequest(val id: String, val workspaceId: String, val projectId: String,
    val kind: String, val payload: DeployPayload, val payloadHash: String, val risk: String,
    val impact: String, val requestedBy: String, val idempotencyKey: String,
    val status: String, val approvalId: String, val approvalCode: String,
    val expiresAt: String, val decidedAt: String?, val decidedBy: String?,
    val result: ActionResult?, val createdAt: String)
data class DeployPayload(val repository: String, val commitSha: String, val environment: String,
    val target: String, val app: String, val deployRequestId: String)
data class ActionResult(val status: String, val sequence: Long, val message: String?,
    val url: String?, val artifactDigest: String?, val reportedAt: String)
