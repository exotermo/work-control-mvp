package com.workcontrol.app.data.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushRoutingTest {
    @Test fun approvalTapRoutesToApprovalAndProject() {
        assertEquals(PushDestination("approval", "a1", "p1"), PushDestination.from(
            mapOf("kind" to "approval", "id" to "a1", "projectId" to "p1")))
    }

    @Test fun actionTapRoutesToDeploys() {
        assertEquals(PushDestination("action", "d1", null), PushDestination.from(
            mapOf("kind" to "action", "id" to "d1")))
    }

    @Test fun unknownOrEmptyPushIsIgnored() {
        assertNull(PushDestination.from(mapOf("kind" to "task", "id" to "t1")))
        assertNull(PushDestination.from(mapOf("kind" to "approval", "id" to "")))
    }
}
