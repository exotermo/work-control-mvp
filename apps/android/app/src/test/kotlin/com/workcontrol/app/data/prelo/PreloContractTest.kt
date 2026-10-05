package com.workcontrol.app.data.prelo

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.http.GET

class PreloContractTest {
    private val gson = Gson()

    @Test
    fun meUsesTheFrozenPreloShape() {
        val me = gson.fromJson(
            """{"userId":"u1","email":"user@example.com","role":"OPERATOR","scopes":["tasks:read"],"workspaceId":"w1","workspaceName":"Prelo","projects":[{"id":"p1","name":"Loja","clientId":null}],"session":{"kind":"mobile","deviceId":"d1","deviceName":"Android"}}""",
            Me::class.java,
        )
        assertEquals("w1", me.workspaceId)
        assertEquals("p1", me.projects.single().id)
        assertEquals("mobile", me.session.kind)
        assertNull(me.projects.single().clientId)
    }

    @Test
    fun taskAndActionUsePreloFields() {
        val task = gson.fromJson(
            """{"id":"t1","description":"Deploy loja","status":"QUEUED","agentId":"general","createdAt":"2026-10-04T00:00:00Z","projectId":"p1"}""",
            Task::class.java,
        )
        val action = gson.fromJson(
            """{"id":"a1","workspaceId":"w1","projectId":"p1","kind":"deploy","payload":{"repository":"exotermo/loja","commitSha":"0123456789abcdef0123456789abcdef01234567","environment":"production","target":"loja.example.com","app":"loja","deployRequestId":"d1"},"payloadHash":"sha256:abc","risk":"HIGH","impact":"Implantar loja","requestedBy":"github:exotermo/loja@main","idempotencyKey":"k1","status":"APPROVED","approvalId":"ap1","approvalCode":"ABCD","expiresAt":"2026-10-04T01:00:00Z","decidedAt":"2026-10-04T00:10:00Z","decidedBy":"user:owner","result":{"status":"RUNNING","sequence":1,"message":"Deploy iniciado","url":null,"artifactDigest":null,"reportedAt":"2026-10-04T00:10:10Z"},"createdAt":"2026-10-04T00:00:00Z"}""",
            ActionRequest::class.java,
        )
        assertEquals("QUEUED", task.status)
        assertEquals("0123456789abcdef0123456789abcdef01234567", action.payload.commitSha)
        assertEquals("ap1", action.approvalId)
        assertEquals("user:owner", action.decidedBy)
        assertEquals(1L, action.result?.sequence)
        assertNull(action.result?.artifactDigest)
    }

    @Test
    fun routesPointAtPreloRatherThanWorkControl() {
        val path = PreloApi::class.java.methods.first { it.name == "me" }.getAnnotation(GET::class.java).value
        assertEquals("api/v1/me", path)
    }
}
