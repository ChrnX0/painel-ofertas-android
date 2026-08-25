package br.com.painelofertas.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.painelofertas.net.PanelLink
import br.com.painelofertas.net.UdpLink
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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SectionLabel("Transmissão")
            Text("Enviar para o painel", style = MaterialTheme.typography.headlineSmall)
        }
        SectionLabel("Álbum", Modifier.padding(top = 16.dp, bottom = 6.dp))
        if (albuns.isEmpty()) {
            Text(
                "Nenhum álbum salvo ainda. Vá na aba Editar, monte o álbum e toque em Salvar — depois volte aqui.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                albuns.forEach { a ->
                    FilterChip(selected = albumSel == a, onClick = { albumSel = a }, enabled = !busy, label = { Text(a) })
                }
            }
        }

        // Tamanho do álbum + "cabe no painel?" (se o painel de destino já é conhecido).
        val albumBytes = remember(albumSel) {
            if (albumSel.isBlank()) null
            else runCatching { container.albums.load(albumSel)?.compile()?.consumo }.getOrNull()
        }
        albumBytes?.let { bytes ->
            val alvo = panels.firstOrNull { it.ip == ip && it.freeMemory > 0 }
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    MonoText("tamanho do álbum: $bytes bytes", size = 11)
                }
            }
        }

        SectionLabel("Conexão", Modifier.padding(top = 18.dp, bottom = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !usarUsb, onClick = { usarUsb = false }, enabled = !busy, label = { Text("Rede (Wi-Fi)") })
            FilterChip(
                selected = usarUsb, onClick = { usarUsb = true }, enabled = usbConnected && !busy,
                label = { Text(if (usbConnected) "USB" else "USB (desconectado)") },
            )
        }

        if (!usarUsb) {
            OutlinedTextField(
                value = ip, onValueChange = { ip = it }, label = { Text("IP do painel") },
                singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
            if (panels.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    panels.forEach { p ->
                        AssistChip(onClick = { ip = p.ip }, label = { Text("${p.name} (${p.ip})") })
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    linkAtual()?.let { vm.enviar(container, albumSel, it, if (usarUsb || ip.isBlank()) null else ip) }
                },
                enabled = !busy && albumSel.isNotBlank() && temDestino,
                shape = ButtonShape,
                modifier = Modifier.weight(1f),
            ) { Text("Enviar") }

            OutlinedButton(
                onClick = { linkAtual()?.let { vm.receber(container, it) } },
                enabled = !busy && temDestino,
                shape = ButtonShape,
                modifier = Modifier.weight(1f),
            ) { Text("Receber") }
        }

        if (vm.busy || vm.progress != null) {
            Box(Modifier.fillMaxWidth().padding(top = 20.dp), contentAlignment = Alignment.Center) {
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

        SectionLabel("Status", Modifier.padding(top = 18.dp, bottom = 6.dp))
        Card(Modifier.fillMaxWidth()) {
            MonoText(
                vm.status.ifBlank { "Pronto." },
                Modifier.padding(12.dp),
                size = 12,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
