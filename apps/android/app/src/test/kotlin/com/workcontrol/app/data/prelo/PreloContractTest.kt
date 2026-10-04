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
            """{"id":"a1","projectId":"p1","kind":"deploy","payload":{"repository":"exotermo/loja","commitSha":"0123456789abcdef0123456789abcdef01234567","environment":"production","target":"loja.example.com","app":"loja","deployRequestId":"d1"},"payloadHash":"sha256:abc","status":"PENDING","impact":"Implantar loja","result":null}""",
            ActionRequest::class.java,
        )
        assertEquals("QUEUED", task.status)
        assertEquals("0123456789abcdef0123456789abcdef01234567", action.payload.commitSha)
        assertNull(action.result)
    }

    @Test
    fun routesPointAtPreloRatherThanWorkControl() {
        val path = PreloApi::class.java.methods.first { it.name == "me" }.getAnnotation(GET::class.java).value
        assertEquals("api/v1/me", path)
    }
}
