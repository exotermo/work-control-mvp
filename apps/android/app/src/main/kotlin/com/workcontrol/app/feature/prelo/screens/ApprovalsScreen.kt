package com.workcontrol.app.feature.prelo.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.workcontrol.app.core.components.Clipping
import com.workcontrol.app.core.components.ClippingFooter
import com.workcontrol.app.core.components.Dateline
import com.workcontrol.app.core.components.DatelineText
import com.workcontrol.app.core.components.EmptySheet
import com.workcontrol.app.core.components.GhostButton
import com.workcontrol.app.core.components.Headline
import com.workcontrol.app.core.components.InkButton
import com.workcontrol.app.core.components.InkField
import com.workcontrol.app.core.components.InkLink
import com.workcontrol.app.core.components.Kicker
import com.workcontrol.app.core.components.SectionCover
import com.workcontrol.app.core.components.SectionTitle
import com.workcontrol.app.core.components.Stamp
import com.workcontrol.app.core.components.stampFor
import com.workcontrol.app.core.designsystem.Prelo
import com.workcontrol.app.core.designsystem.PreloType
import com.workcontrol.app.feature.prelo.PreloController
import com.workcontrol.app.feature.prelo.expiresIn
import com.workcontrol.app.feature.prelo.pageState
import com.workcontrol.app.feature.prelo.relativeTime
import com.workcontrol.app.feature.prelo.rows
import com.workcontrol.app.feature.prelo.str
import kotlinx.coroutines.delay

/** "tool:send_whatsapp" → "Send whatsapp"; keeps whatever the server sends readable. */
internal fun scopeTitle(scope: String?): String =
    scope?.substringAfter(':')?.replace('_', ' ')?.replace('.', ' ')?.replaceFirstChar { it.uppercase() } ?: "Aprovação"

@Composable
fun ApprovalsScreen(c: PreloController, padding: PaddingValues) {
    if (c.detailId != null) { ApprovalDetail(c, padding); return }
    val all = c.data.rows()
    val pending = all.filter { it.str("status") == "PENDING" }
    val decided = all.filterNot { it.str("status") == "PENDING" }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item(key = "cover") { SectionCover("Seção · Aprovações", "Mesa de decisão", deck = "Nada de risco acontece sem o seu carimbo.") }
        pageState(c, c.data != null)
        if (c.data != null && c.error == null) {
            item(key = "pending-title") { SectionTitle("Esperando você", count = pending.size) }
            if (pending.isEmpty()) item(key = "pending-empty") { EmptySheet("Nenhuma aprovação pendente.") }
            items(pending.withIndex().toList(), key = { (_, a) -> "a-" + (a.str("id") ?: "") }) { (i, a) -> ApprovalClipping(c, a, i) }
            if (decided.isNotEmpty()) {
                item(key = "decided-title") { SectionTitle("Decididas") }
                items(decided.withIndex().toList(), key = { (_, a) -> "a-" + (a.str("id") ?: "") }) { (i, a) -> ApprovalClipping(c, a, i + pending.size) }
            }
        }
    }
}

@Composable
private fun ApprovalClipping(c: PreloController, a: JsonElement, index: Int) {
    val id = a.str("id") ?: return
    val first = c.firstSight("approval-$id")
    val (label, tone) = stampFor(a.str("status"))
    Clipping(seed = id.hashCode(), index = index, animateEntrance = first, onClick = { c.openItem(id) }, onClickLabel = "Abrir aprovação") {
        Kicker(if (a.str("actionRequestId") != null) "Pedido externo" else "Ferramenta de agente")
        Headline(scopeTitle(a.str("scope")))
        Dateline {
            DatelineText(
                (if (a.str("status") == "PENDING") expiresIn(a.str("expiresAt")) else relativeTime(a.str("decidedAt") ?: a.str("requestedAt")))
                    ?: "—",
                Modifier.weight(1f),
            )
            Stamp(label, tone, animate = first)
        }
        a.str("shortCode")?.let { code ->
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("CÓDIGO", style = PreloType.Telemetry, color = Prelo.colors.faint)
                Spacer(Modifier.weight(1f))
                Text(code, Modifier.semantics { contentDescription = "Código $code" }, style = PreloType.Code.copy(fontSize = PreloType.Code.fontSize * 0.7f),
                    color = Prelo.colors.ink)
            }
        }
    }
}

@Composable
private fun ApprovalDetail(c: PreloController, padding: PaddingValues) {
    val a = c.detail
    val id = c.detailId ?: return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(id) { while (true) { delay(1_000); now = System.currentTimeMillis() } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "back") { InkLink("← Voltar à mesa", onClick = { c.closeDetail() }) }
        pageState(c, a != null)
        if (a != null) item(key = "card") {
            val pending = a.str("status") == "PENDING"
            val (label, tone) = stampFor(a.str("status"))
            Clipping(seed = id.hashCode(), animateEntrance = false) {
                Kicker(if (a.str("actionRequestId") != null) "Pedido externo · deploy" else "Ferramenta de agente")
                Headline(scopeTitle(a.str("scope")), maxLines = 4)
                Dateline {
                    DatelineText((if (pending) expiresIn(a.str("expiresAt"), now) else relativeTime(a.str("decidedAt"))) ?: "—", Modifier.weight(1f))
                    Stamp(label, tone)
                }
                a.str("shortCode")?.let { code ->
                    Column(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CÓDIGO DO WHATSAPP", style = PreloType.Telemetry, color = Prelo.colors.faint)
                        Text(code, style = PreloType.Code, color = Prelo.colors.ink)
                    }
                }
                a.str("decidedBy")?.let { Text("Decidida por $it", Modifier.padding(top = 8.dp), style = PreloType.Dateline, color = Prelo.colors.text) }
                if (pending && c.stepUpId != id) ClippingFooter {
                    GhostButton("Negar", onClick = { c.deny(id) }, enabled = !c.busy, danger = true)
                    Spacer(Modifier.weight(1f))
                    InkButton("Aprovar", onClick = { c.approve(id) }, enabled = !c.busy)
                }
            }
        }
        if (c.stepUpId == id) item(key = "step-up") { StepUp(c, id) }
    }
}

/** HIGH risk from the app needs a fresh TOTP (contract G9): the server asks, we confirm. */
@Composable
private fun StepUp(c: PreloController, id: String) {
    var totp by remember { mutableStateOf("") }
    Clipping(seed = id.hashCode() + 1, animateEntrance = true, tape = true) {
        Kicker("Confirmação reforçada")
        Headline("Risco alto: confirme com o código do autenticador")
        Text("Confirme esta aprovação com o código TOTP.", style = PreloType.Body, color = Prelo.colors.text)
        InkField(totp, { if (it.length <= 6 && it.all(Char::isDigit)) totp = it }, "Código TOTP",
            Modifier.fillMaxWidth().padding(top = 12.dp), mono = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
        ClippingFooter {
            InkLink("Cancelar", onClick = { c.cancelStepUp() })
            Spacer(Modifier.weight(1f))
            InkButton("Confirmar aprovação", onClick = { c.confirmStepUp(id, totp) { totp = "" } }, enabled = !c.busy && totp.length == 6)
        }
    }
}
