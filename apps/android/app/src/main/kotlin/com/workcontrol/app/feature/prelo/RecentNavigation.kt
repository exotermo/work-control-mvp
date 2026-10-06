package com.workcontrol.app.feature.prelo

import com.workcontrol.app.data.prelo.RecentTouch

internal fun recentDestination(kind: String?, id: String?, projectId: String?): Destination? {
    if (id.isNullOrBlank()) return null
    return when (kind) {
        "TASK" -> Destination(Page.TASKS, id, projectId)
        "PROJECT" -> Destination(Page.PROJECT, projectId = id)
        "CLIENT" -> Destination(Page.CLIENT, id, projectId)
        else -> null
    }
}

internal class RecentTracker {
    private var last: RecentTouch? = null
    fun shouldSend(touch: RecentTouch): Boolean {
        if (touch == last) return false
        last = touch
        return true
    }
}
