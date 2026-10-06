package com.workcontrol.app.feature.prelo

import com.google.gson.JsonElement
import java.time.OffsetDateTime

internal data class TimelineMerge(val rows: List<JsonElement>, val endReached: Boolean)

/** The cursor is exclusive. A repeated or newer page must not duplicate the visible history. */
internal fun mergeTimeline(current: List<JsonElement>, batch: List<JsonElement>): TimelineMerge {
    val before = current.lastOrNull()?.str("at")
    val cursor = before?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }
    if (before != null && cursor == null) return TimelineMerge(current, true)
    val seen = current.map { it.str("kind") to it.str("id") }.toMutableSet()
    val fresh = batch.filter { entry ->
        val at = entry.str("at")?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }
        at != null && (cursor == null || at.isBefore(cursor)) &&
            seen.add(entry.str("kind") to entry.str("id"))
    }
    return TimelineMerge(current + fresh, batch.isEmpty() || fresh.isEmpty())
}
