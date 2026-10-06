package com.workcontrol.app.feature.prelo.screens

/** Only presentation links; the server decides whether this client may be read. */
internal fun contactUri(kind: String?, value: String?, optedOut: Boolean): String? {
    val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return when (kind) {
        "PHONE" -> "tel:$raw"
        "EMAIL" -> "mailto:$raw"
        "WHATSAPP" -> if (optedOut) null else raw.filter(Char::isDigit).takeIf { it.isNotEmpty() }?.let { "https://wa.me/$it" }
        else -> null
    }
}
