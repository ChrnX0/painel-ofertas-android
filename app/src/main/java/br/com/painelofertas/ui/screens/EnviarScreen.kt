package br.com.painelofertas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.painelofertas.net.PanelLink
import br.com.painelofertas.net.UdpLink
import br.com.painelofertas.ui.components.Appear
import br.com.painelofertas.ui.components.ButtonShape
import br.com.painelofertas.ui.components.MonoText
import br.com.painelofertas.ui.components.SectionLabel
import br.com.painelofertas.ui.rememberContainer
import br.com.painelofertas.ui.vm.AppViewModel
import br.com.painelofertas.ui.vm.SendViewModel

@Composable
fun EnviarScreen() {
    val container = rememberContainer()
    val vm: SendViewModel = viewModel()
    val nav: AppViewModel = viewModel()
    val haptic = LocalHapticFeedback.current
    val panels by container.panels.panels.collectAsState()
    val usbConnected by container.usb.connected.collectAsState()
    val albuns by container.albums.names.collectAsState()

    var albumSel by remember { mutableStateOf(albuns.firstOrNull() ?: "") }
    var ip by remember { mutableStateOf("") }
    var usarUsb by remember { mutableStateOf(false) }

    // fluxo "Salvar e enviar": pré-seleciona o álbum salvo no editor
    LaunchedEffect(Unit) { nav.consumePendingSend()?.let { albumSel = it } }

    val busy = vm.busy
    val temDestino = if (usarUsb) usbConnected else ip.isNotBlank()

    fun linkAtual(): PanelLink? =
        if (usarUsb) container.usb.link.value
        else if (ip.isNotBlank()) UdpLink(ip, container.udp) else null

    // Tamanho do álbum + "cabe no painel?" (se o painel de destino já é conhecido).
    val albumBytes = remember(albumSel) {
        if (albumSel.isBlank()) null
        else runCatching { container.albums.load(albumSel)?.compile()?.consumo }.getOrNull()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Appear {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SectionLabel("Transmissão")
                Text("Enviar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            }
        }

        // ===== PASSO 1 — Álbum =====
        Appear(delayMillis = 50) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StepHeader(1, "O que enviar")
                    if (albuns.isEmpty()) {
                        Text(
                            "Nenhum álbum salvo ainda. Vá em Editar, monte as telas e toque em Salvar — depois volte aqui.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            albuns.forEach { a ->
                                FilterChip(selected = albumSel == a, onClick = { albumSel = a }, enabled = !busy, label = { Text(a) })
                            }
                        }
                        albumBytes?.let { bytes ->
                            val alvo = panels.firstOrNull { it.ip == ip && it.freeMemory > 0 }
                            if (alvo != null) {
                                val frac = (bytes.toFloat() / alvo.freeMemory).coerceIn(0f, 1f)
                                val cabe = bytes < alvo.freeMemory
                                LinearProgressIndicator(
                                    progress = { frac },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = if (cabe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                )
                                MonoText(
                                    (if (cabe) "cabe" else "NÃO cabe") + " · usa $bytes de ${alvo.freeMemory} B (${(frac * 100).toInt()}%)",
                                    size = 11,
                                    color = if (cabe) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                                )
                            } else {
                                MonoText("tamanho: $bytes bytes", size = 11)
                            }
                        }
                    }
                }
            }
        }

        // ===== PASSO 2 — Destino =====
        Appear(delayMillis = 100) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StepHeader(2, "Para onde")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !usarUsb, onClick = { usarUsb = false }, enabled = !busy, label = { Text("Rede (Wi-Fi)") })
                        FilterChip(
                            selected = usarUsb, onClick = { usarUsb = true }, enabled = usbConnected && !busy,
                            label = { Text(if (usbConnected) "USB" else "USB (desconectado)") },
                        )
                    }
                    androidx.compose.animation.AnimatedVisibility(!usarUsb) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = ip, onValueChange = { ip = it }, label = { Text("IP do painel") },
                                singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                            )
                            if (panels.isEmpty()) {
                                Text(
                                    "Nenhum painel localizado ainda. Toque em cada painel na aba Painéis, ou digite o IP.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    panels.forEach { p ->
                                        AssistChip(onClick = { ip = p.ip }, label = { Text("${p.name} (${p.ip})") })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ===== PASSO 3 — Ações =====
        Appear(delayMillis = 150) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        linkAtual()?.let { vm.enviar(container, albumSel, it, if (usarUsb || ip.isBlank()) null else ip, viaUsb = usarUsb) }
                    },
                    enabled = !busy && albumSel.isNotBlank() && temDestino,
                    shape = ButtonShape,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text("Enviar para o painel") }

                OutlinedButton(
                    onClick = { linkAtual()?.let { vm.receber(container, it, viaUsb = usarUsb) } },
                    enabled = !busy && temDestino,
                    shape = ButtonShape,
                    modifier = Modifier.fillMaxWidth(),
                ) { Icon(Icons.Filled.Download, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text("Receber (ler o que está no painel)") }
            }
        }

        androidx.compose.animation.AnimatedVisibility(vm.busy || vm.progress != null) {
            Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                Box(contentAlignment = Alignment.Center) {
                    val p = vm.progress
                    if (p != null) {
                        CircularProgressIndicator(
                            progress = { p },
                            modifier = Modifier.size(132.dp),
                            strokeWidth = 10.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            strokeCap = StrokeCap.Round,
                        )
                        MonoText(
                            "${(p * 100).toInt()}%",
                            size = 24,
                            weight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(132.dp),
                            strokeWidth = 10.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                    }
                }
            }
        }

        Appear(delayMillis = 200) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel("Status")
                Card(Modifier.fillMaxWidth()) {
                    MonoText(
                        vm.status.ifBlank { "Pronto." },
                        Modifier.padding(14.dp),
                        size = 12,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Cabeçalho de passo: bolinha numerada + título (estilo assistente One UI). */
@Composable
private fun StepHeader(n: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text("$n", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
        }
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}
