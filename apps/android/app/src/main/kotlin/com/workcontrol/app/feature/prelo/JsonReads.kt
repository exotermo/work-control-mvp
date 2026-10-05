package com.workcontrol.app.feature.prelo

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import retrofit2.HttpException

// Read models stay as the server's JSON (the server owns state and authorization); these helpers
// only read fields for display.

internal fun JsonElement?.obj(): JsonObject? = this?.takeIf { it.isJsonObject }?.asJsonObject

internal fun JsonElement?.str(name: String): String? = this.obj()?.get(name)?.takeIf { !it.isJsonNull }?.let {
    if (it.isJsonPrimitive) it.asString else null
}

internal fun JsonElement?.child(name: String): JsonElement? = this.obj()?.get(name)?.takeIf { !it.isJsonNull }

internal fun JsonElement?.rows(): List<JsonElement> = when {
    this == null || isJsonNull -> emptyList()
    isJsonArray -> asJsonArray.toList()
    isJsonObject -> (obj()?.get("items") ?: obj()?.get("files")).rows()
    else -> emptyList()
}

internal fun displayError(failure: Exception): String = when (failure) {
    is HttpException -> when (failure.code()) {
        401 -> "Entre novamente para continuar."
        403 -> "Sem acesso."
        404 -> "Recurso não encontrado."
        else -> "Erro do servidor (${failure.code()})."
    }
    else -> "Sem conexão com o Prelo."
}

/** "há 3 min" style relative time from an RFC3339 timestamp, like the dashboard's when(). */
internal fun relativeTime(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    val instant = runCatching { java.time.OffsetDateTime.parse(iso).toInstant() }.getOrNull() ?: return null
    val minutes = java.time.Duration.between(instant, java.time.Instant.now()).toMinutes()
    return when {
        minutes < 1 -> "agora"
        minutes < 60 -> "há $minutes min"
        minutes < 24 * 60 -> "há ${(minutes + 30) / 60} h"
        else -> java.time.format.DateTimeFormatter.ofPattern("dd MMM", java.util.Locale("pt", "BR"))
            .withZone(java.time.ZoneId.systemDefault()).format(instant)
    }
}

/** "expira em 12 min" countdown text for approvals. */
internal fun expiresIn(iso: String?, nowMillis: Long = System.currentTimeMillis()): String? {
    val instant = runCatching { java.time.OffsetDateTime.parse(iso).toInstant() }.getOrNull() ?: return null
    val seconds = (instant.toEpochMilli() - nowMillis) / 1000
    return when {
        seconds <= 0 -> "expirada"
        seconds < 60 -> "expira em ${seconds}s"
        else -> "expira em ${seconds / 60} min"
    }
}
