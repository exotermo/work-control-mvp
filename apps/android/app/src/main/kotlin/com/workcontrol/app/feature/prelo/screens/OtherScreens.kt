package com.workcontrol.app.feature.prelo.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.components.Clipping
import com.workcontrol.app.core.components.ClippingFooter
import com.workcontrol.app.core.components.Dateline
import com.workcontrol.app.core.components.DatelineText
import com.workcontrol.app.core.components.EmptySheet
import com.workcontrol.app.core.components.GhostButton
import com.workcontrol.app.core.components.Headline
import com.workcontrol.app.core.components.InkLink
import com.workcontrol.app.core.components.Kicker
import com.workcontrol.app.core.components.SectionCover
import com.workcontrol.app.core.components.SectionTitle
import com.workcontrol.app.core.components.Stamp
import com.workcontrol.app.core.components.StatusDot
import com.workcontrol.app.core.components.stampFor
import com.workcontrol.app.core.designsystem.Prelo
import com.workcontrol.app.core.designsystem.PreloType
import com.workcontrol.app.feature.prelo.Page
import com.workcontrol.app.feature.prelo.PreloController
import com.workcontrol.app.feature.prelo.child
import com.workcontrol.app.feature.prelo.pageState
import com.workcontrol.app.feature.prelo.relativeTime
import com.workcontrol.app.feature.prelo.rows
import com.workcontrol.app.feature.prelo.str

/** Deploys of the project (contract G8, read only): repo@sha, environment → target, result stamp. */
@Composable
fun DeploysScreen(c: PreloController, padding: PaddingValues) {
    val deploys = c.data.rows()
    val uri = LocalUriHandler.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item(key = "cover") { SectionCover("${c.projectName ?: "Projeto"} · Deploys", "Expedição", deck = "Pedidos de implantação e o que aconteceu com cada um.") }
        if (c.selected == null) item(key = "no-project") { EmptySheet("Escolha um projeto", "Os deploys são por projeto.") }
        pageState(c, c.data != null)
        if (c.data != null && deploys.isEmpty() && c.error == null) item(key = "empty") { EmptySheet("Nenhum deploy pedido neste projeto.") }
        items(deploys.withIndex().toList(), key = { (_, d) -> "d-" + (d.str("id") ?: "") }) { (i, d) ->
            val id = d.str("id") ?: ""
            val payload = d.child("payload")
            val result = d.child("result")
            val first = c.firstSight("deploy-$id")
            val (label, tone) = stampFor(result.str("status") ?: d.str("status"))
            Clipping(seed = id.hashCode(), index = i, animateEntrance = first) {
                Kicker(listOfNotNull(payload.str("environment"), payload.str("app")).joinToString(" · ").ifBlank { "Deploy" })
                Headline(d.str("impact") ?: "Deploy")
                Text("${payload.str("repository") ?: "—"}@${payload.str("commitSha")?.take(8) ?: "—"}", style = PreloType.Teletype,
                    color = Prelo.colors.ink)
                Text("→ ${payload.str("target") ?: "—"}", Modifier.padding(bottom = 8.dp), style = PreloType.Teletype, color = Prelo.colors.text)
                Dateline {
                    DatelineText(relativeTime(result.str("reportedAt") ?: d.str("createdAt")) ?: "—", Modifier.weight(1f))
                    Stamp(label, tone, animate = first)
                }
                result.str("message")?.let { Text(it, Modifier.padding(top = 8.dp), style = PreloType.Body, color = Prelo.colors.text) }
                result.str("url")?.let { url ->
                    ClippingFooter { InkLink("Abrir $url", onClick = { runCatching { uri.openUri(url) } }) }
                }
            }
        }
    }
}

/** "Mais" as a magazine table of contents: section name, dotted leader, page number. */
@Composable
fun MoreScreen(c: PreloController, padding: PaddingValues) {
    val entries = listOf(
        Triple(Page.CLIENTS, "Clientes", "Carteira de clientes e dossiês"),
        Triple(Page.SERVERS, "Sala de máquinas", "Servidores do projeto e saúde"),
        Triple(Page.PIPELINE, "Linha de montagem", "Pipeline: o que cada tarefa espera"),
        Triple(Page.FILES, "Arquivo", "Documentos do projeto"),
        Triple(Page.ACCOUNT, "Sua conta", "Notificações, aparelhos, tema e saída"),
    )
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item(key = "cover") { SectionCover("Índice", "Nesta edição", deck = "Tudo o mais que o Prelo guarda para você.") }
        item(key = "rule") { SectionTitle("Sumário") }
        items(entries, key = { it.first.name }) { (page, title, hint) ->
            TocRow(title, hint, "p. %02d".format(page.ordinal + 1)) { c.open(page) }
        }
    }
}

@Composable
private fun TocRow(title: String, hint: String, number: String, onClick: () -> Unit) {
    val palette = Prelo.colors
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).heightIn(min = 64.dp).padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleLarge, color = palette.ink)
            Spacer(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .heightIn(min = 6.dp)
                    .drawBehind {
                        drawLine(palette.rule.copy(alpha = 0.5f), Offset(0f, size.height - 2.dp.toPx()), Offset(size.width, size.height - 2.dp.toPx()),
                            1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.5.dp.toPx(), 5.dp.toPx())))
                    },
            )
            Text(number, style = PreloType.Telemetry, color = palette.accentInk)
        }
        Text(hint, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = palette.text)
    }
}

/** Machines as telemetry rows: status dot, name, host, last check; detail with health check. */
@Composable
fun ServersScreen(c: PreloController, padding: PaddingValues) {
    val servers = c.data.rows()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item(key = "back") { InkLink("← Voltar", onClick = { c.back() }) }
        item(key = "cover") { SectionCover("${c.projectName ?: "Projeto"} · Máquinas", "Sala de máquinas", deck = "Os servidores deste projeto, vistos daqui.") }
        pageState(c, c.data != null)
        val open = c.detailId
        if (open != null) {
            val s = c.detail
            if (s != null) item(key = "detail") {
                Clipping(seed = open.hashCode(), animateEntrance = false) {
                    Kicker("Máquina")
                    Headline(s.str("name") ?: "Máquina")
                    Text("${s.str("sshUser") ?: ""}@${s.str("host") ?: "—"}:${s.str("sshPort") ?: "22"}", style = PreloType.Teletype, color = Prelo.colors.ink)
                    Dateline {
                        DatelineText("última verificação ${relativeTime(s.str("lastCheckedAt")) ?: "—"}", Modifier.weight(1f))
                        val (label, tone) = stampFor(c.health.str("status") ?: s.str("lastStatus"))
                        Stamp(label, tone)
                    }
                    c.health?.let { h ->
                        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Telemetry("UPTIME", h.str("uptime"))
                            Telemetry("MEMÓRIA", h.str("memoryUsedMb")?.let { "$it / ${h.str("memoryTotalMb")} MB" })
                            Telemetry("DISCO", h.str("diskUsedPercent")?.let { "$it%" })
                            h.str("error")?.let { Text(it, style = PreloType.Body, color = Prelo.colors.bad) }
                        }
                    } ?: s.str("lastError")?.let { Text(it, Modifier.padding(top = 8.dp), style = PreloType.Body, color = Prelo.colors.bad) }
                    ClippingFooter {
                        InkLink("Fechar", onClick = { c.closeDetail() })
                        Spacer(Modifier.weight(1f))
                        GhostButton("Verificar saúde", onClick = { c.healthCheck(open) }, enabled = !c.busy)
                    }
                }
            }
        }
        if (c.data != null && servers.isEmpty() && c.error == null) item(key = "empty") { EmptySheet("Nenhuma máquina neste projeto.") }
        items(servers, key = { "s-" + (it.str("id") ?: "") }) { s ->
            val id = s.str("id") ?: return@items
            val (label, tone) = stampFor(s.str("lastStatus"))
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = { c.openItem(id) }).heightIn(min = 56.dp).padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatusDot(tone)
                Column(Modifier.weight(1f)) {
                    Text(s.str("name") ?: "Máquina", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = Prelo.colors.ink)
                    Text(s.str("host") ?: "—", style = PreloType.Telemetry, color = Prelo.colors.text)
                }
                Stamp(label, tone, rotation = -4f, animate = false)
            }
        }
    }
}

@Composable
private fun Telemetry(label: String, value: String?) {
    if (value == null) return
    Row {
        Text(label, Modifier.weight(1f), style = PreloType.Telemetry, color = Prelo.colors.faint)
        Text(value, style = PreloType.Telemetry, color = Prelo.colors.ink)
    }
}

/** Pipeline tree: what each task is waiting on right now. */
@Composable
fun PipelineScreen(c: PreloController, padding: PaddingValues) {
    val roots = c.data.rows()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item(key = "back") { InkLink("← Voltar", onClick = { c.back() }) }
        item(key = "cover") { SectionCover("${c.projectName ?: "Projeto"} · Pipeline", "Linha de montagem", deck = "Cada tarefa e o que ela espera agora.") }
        pageState(c, c.data != null)
        if (c.data != null && roots.isEmpty() && c.error == null) item(key = "empty") { EmptySheet("Nada em produção agora.") }
        if (roots.isNotEmpty()) item(key = "tree") {
            Column(Modifier.padding(top = 12.dp)) { TreeRows(roots, depth = 0, onOpen = { c.openTask(it) }, statusField = "pipelineStatus") }
        }
    }
}

/** Project files: name, size, date; download goes through the system file picker. */
@Composable
fun FilesScreen(c: PreloController, padding: PaddingValues, onDownload: (String, String) -> Unit) {
    val files = c.data.rows()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item(key = "back") { InkLink("← Voltar", onClick = { c.back() }) }
        item(key = "cover") { SectionCover("${c.projectName ?: "Projeto"} · Arquivos", "Arquivo do projeto", deck = "Guardados cifrados no Prelo.") }
        if (c.selected == null) item(key = "no-project") { EmptySheet("Escolha um projeto", "Os arquivos são por projeto.") }
        pageState(c, c.data != null)
        if (c.data != null && files.isEmpty() && c.error == null) item(key = "empty") { EmptySheet("Nenhum arquivo neste projeto.") }
        items(files, key = { "f-" + (it.str("id") ?: "") }) { f ->
            val id = f.str("id") ?: return@items
            val name = f.str("name") ?: "arquivo"
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text((f.str("kind") ?: "doc").uppercase().take(4), style = PreloType.Stamp, color = Prelo.colors.kicker)
                Column(Modifier.weight(1f)) {
                    Text(name, style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = Prelo.colors.ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(f.str("sizeBytes")?.toLongOrNull()?.let(::humanSize), relativeTime(f.str("createdAt"))).joinToString(" · "),
                        style = PreloType.Dateline, color = Prelo.colors.faint)
                }
                InkLink("Baixar", onClick = { onDownload(id, name) })
            }
        }
    }
}

private fun humanSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
}
