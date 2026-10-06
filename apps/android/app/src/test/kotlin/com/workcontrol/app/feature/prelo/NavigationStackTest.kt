package com.workcontrol.app.feature.prelo

import com.workcontrol.app.data.prelo.RecentTouch
import com.google.gson.JsonParser
import com.workcontrol.app.feature.prelo.screens.contactUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationStackTest {
    @Test fun projectTaskBackReturnsThroughProjectToHome() {
        val stack = NavigationStack()
        assertFalse(stack.back())
        stack.navigate(Destination(Page.PROJECT, projectId = "p1"))
        stack.navigate(Destination(Page.TASKS, projectId = "p1"))
        stack.navigate(Destination(Page.TASKS, "t1", "p1"))
        assertTrue(stack.canGoBack)
        assertTrue(stack.back()); assertEquals(Destination(Page.TASKS, projectId = "p1"), stack.current)
        assertTrue(stack.back()); assertEquals(Destination(Page.PROJECT, projectId = "p1"), stack.current)
        assertTrue(stack.back()); assertEquals(Page.HOME, stack.current.page)
        assertFalse(stack.back())
    }

    @Test fun bottomTabDoesNotPushHistory() {
        val stack = NavigationStack()
        stack.navigate(Destination(Page.PROJECT, projectId = "p1"))
        stack.tab(Page.APPROVALS)
        assertTrue(stack.back())
        assertEquals(Page.HOME, stack.current.page)
        assertFalse(stack.back())
    }

    @Test fun recentRoutesAndConsecutiveTouches() {
        assertEquals(Destination(Page.TASKS, "t1", "p1"), recentDestination("TASK", "t1", "p1"))
        assertEquals(Destination(Page.PROJECT, projectId = "p2"), recentDestination("PROJECT", "p2", null))
        assertEquals(Destination(Page.CLIENT, "c1", null), recentDestination("CLIENT", "c1", null))
        val tracker = RecentTracker()
        assertTrue(tracker.shouldSend(RecentTouch("TASK", "t1")))
        assertFalse(tracker.shouldSend(RecentTouch("TASK", "t1")))
        assertTrue(tracker.shouldSend(RecentTouch("CLIENT", "c1")))
        assertTrue(tracker.shouldSend(RecentTouch("TASK", "t1")))
    }

    @Test fun contactLinksRespectOptOut() {
        assertEquals("tel:+5511999999999", contactUri("PHONE", "+5511999999999", true))
        assertEquals("mailto:ana@example.com", contactUri("EMAIL", "ana@example.com", true))
        assertEquals("https://wa.me/5511999999999", contactUri("WHATSAPP", "+55 (11) 99999-9999", false))
        assertEquals(null, contactUri("WHATSAPP", "+5511999999999", true))
    }

    @Test fun timelineRejectsRepeatedOrNewerPage() {
        val first = JsonParser.parseString("""{"kind":"PROJECT","id":"p1","at":"2026-10-06T10:00:00.123456789-03:00"}""")
        val repeated = JsonParser.parseString("""{"kind":"PROJECT","id":"p1","at":"2026-10-06T10:00:01.123456789-03:00"}""")
        val merged = mergeTimeline(listOf(first), listOf(repeated))
        assertEquals(1, merged.rows.size)
        assertTrue(merged.endReached)
    }
}
