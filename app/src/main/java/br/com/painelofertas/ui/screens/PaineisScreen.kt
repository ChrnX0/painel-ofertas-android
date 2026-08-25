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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.painelofertas.data.Panel
import br.com.painelofertas.data.PanelStatus
import br.com.painelofertas.data.SyncState
import br.com.painelofertas.net.LocalIp
import br.com.painelofertas.net.UdpLink
import br.com.painelofertas.ui.components.ButtonShape
import br.com.painelofertas.ui.components.EmptyState
import br.com.painelofertas.ui.components.MonoText
import br.com.painelofertas.ui.components.SectionLabel
import br.com.painelofertas.ui.rememberContainer
import br.com.painelofertas.usb.WifiModuleConfigurator
import kotlinx.coroutines.launch

@Composable
fun PaineisScreen() {
    val container = rememberContainer()
    val scope = rememberCoroutineScope()
    val panels by container.panels.panels.collectAsState()
    val usbConnected by container.usb.connected.collectAsState()
    var localIp by remember { mutableStateOf(container.settings.localIp.ifBlank { LocalIp.detect() ?: "" }) }
    var status by remember { mutableStateOf("") }

    // Auto-conectar: ao abrir a aba, re-anuncia na rede (painéis na mesma rede aparecem sozinhos).
    LaunchedEffect(Unit) { container.autoConnect() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SectionLabel("Dispositivos")
            Text("Painéis", style = MaterialTheme.typography.headlineSmall)
        }
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = localIp, onValueChange = { localIp = it; container.settings.localIp = it },
                label = { Text("IP local") }, singleLine = true, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(8.dp))
            Button(onClick = {
                scope.launch { status = "Procurando…"; container.discovery.scan(localIp); status = "Varredura enviada." }
            }, shape = ButtonShape) { Text("Procurar") }
        }
        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (panels.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Tv,
                title = "Procurando painéis…",
                subtitle = "Ao abrir, o app varre a rede sozinho. O painel precisa estar ligado e na mesma rede Wi-Fi. Toque em Procurar para varrer de novo.",
            )
        } else {
            panels.forEach { p ->
                fun cmd(text: String) = scope.launch { UdpLink(p.ip, container.udp).sendText(text) }
                PanelCard(
                    p,
                    onApply = { v, sensor ->
                        container.panels.setBrightness(p.id, v)
                        container.panels.setSensorAuto(p.id, sensor)
                        cmd("INICIAR=${v + if (sensor) 128 else 0}")
                    },
                    onIdentificar = { cmd("INICIAR=228") },
                    onLigar = { cmd("INICIAR=${p.brightness + if (p.sensorAuto) 128 else 0}") },
                    onDesligar = { cmd("ONOFF=0") },
                    onRename = { container.panels.rename(p.id, it) },
                )
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        WifiConfigCard(usbConnected, localIp, container, scope)
    }
}

@Composable
private fun PanelCard(
    p: Panel,
    onApply: (Int, Boolean) -> Unit,
    onIdentificar: () -> Unit,
    onLigar: () -> Unit,
    onDesligar: () -> Unit,
    onRename: (String) -> Unit,
) {
    var nome by remember(p.id) { mutableStateOf(p.name) }
    var brilho by remember(p.id, p.brightness) { mutableFloatStateOf(p.brightness.toFloat()) }
    var sensor by remember(p.id, p.sensorAuto) { mutableStateOf(p.sensorAuto) }

    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // status + nome
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(11.dp).background(statusColor(p.status), CircleShape)
                        .semantics { contentDescription = "Status: ${statusLabel(p.status)}" },
                )
                OutlinedTextField(nome, { nome = it }, label = { Text("Nome do painel") }, singleLine = true, modifier = Modifier.weight(1f))
            }

            SyncBadge(p.syncState)

            MonoText("IP ${p.ip} · livre ${p.freeMemory} B · CRC 0x%04X".format(p.crcPanel), size = 11)

            // brilho + sensor de luz
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Brilho ${brilho.toInt()}%", Modifier.weight(1f))
                MonoText("sensor: ${p.intensity}%", size = 10, color = MaterialTheme.colorScheme.tertiary)
            }
            Slider(value = brilho, onValueChange = { brilho = it }, valueRange = 0f..100f)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Switch(sensor, { sensor = it })
                Column(Modifier.weight(1f)) {
                    Text("Auto-brilho (sensor de luz)", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "O painel se ajusta sozinho à luz da loja.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onApply(brilho.toInt(), sensor) }, shape = ButtonShape) { Text("Aplicar") }
                OutlinedButton(onClick = onLigar, shape = ButtonShape) { Text("Ligar") }
                OutlinedButton(onClick = onDesligar, shape = ButtonShape) { Text("Desligar") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onIdentificar, shape = ButtonShape) { Text("Identificar") }
                OutlinedButton(onClick = { onRename(nome) }, shape = ButtonShape) { Text("Renomear") }
            }
        }
    }
}

/** Selo de sincronismo: o painel exibe exatamente o álbum que enviamos? (CRC). */
@Composable
private fun SyncBadge(state: SyncState) {
    val cs = MaterialTheme.colorScheme
    val (bg, fg, txt) = when (state) {
        SyncState.SYNCED -> Triple(Color(0x2634D399), Color(0xFF34D399), "✓ Sincronizado")
        SyncState.OUTDATED -> Triple(Color(0x26FBBF24), Color(0xFFFBBF24), "⚠ Desatualizado — reenvie")
        SyncState.UNKNOWN -> Triple(cs.surfaceContainerHigh, cs.onSurfaceVariant, "Sem referência de envio")
    }
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(txt, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

@Composable
private fun WifiConfigCard(
    usbConnected: Boolean,
    localIp: String,
    container: br.com.painelofertas.AppContainer,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    var ssids by remember { mutableStateOf<List<String>>(emptyList()) }
    var ssidSel by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }

    Text("Configurar WiFi do painel (via USB)", style = MaterialTheme.typography.titleMedium)
    if (!usbConnected) {
        Text("Conecte o painel por USB (OTG) para configurar a rede.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column(Modifier.padding(top = 8.dp)) {
        OutlinedButton(onClick = {
            val link = container.usb.link.value ?: return@OutlinedButton
            scope.launch {
                msg = "Lendo configuração…"
                val cfg = WifiModuleConfigurator(link).readConfig()
                ssids = cfg.ssids
                ssidSel = cfg.currentSsid
                msg = "Redes: ${ssids.size}. Rede atual: ${cfg.currentSsid.ifBlank { "—" }}"
            }
        }, shape = ButtonShape) { Text("Ler config / escanear redes") }

        if (ssids.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ssids.forEach { s ->
                    FilterChip(selected = ssidSel == s, onClick = { ssidSel = s }, label = { Text(s) })
                }
            }
        }
        OutlinedTextField(senha, { senha = it }, label = { Text("Senha da rede") },
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        Button(
            onClick = {
                val link = container.usb.link.value ?: return@Button
                scope.launch {
                    msg = "Entrando na rede…"
                    val ok = WifiModuleConfigurator(link).join(
                        ssid = ssidSel, password = senha, localIp = localIp,
                        dhcp = container.settings.dhcp,
                    )
                    msg = if (ok) "✅ Painel configurado na rede." else "❌ Falha ao entrar na rede."
                }
            },
            enabled = ssidSel.isNotBlank(),
            shape = ButtonShape,
            modifier = Modifier.padding(top = 4.dp),
        ) { Text("Entrar na rede") }
        if (msg.isNotBlank()) Text(msg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
    }
}

private fun statusColor(s: PanelStatus): Color = when (s) {
    PanelStatus.ONLINE -> Color(0xFF34D399)
    PanelStatus.DEGRADED -> Color(0xFFFB8C00)
    PanelStatus.OFFLINE -> Color(0xFF7A8699)
    PanelStatus.USB -> Color(0xFF3B9EFF)
}

private fun statusLabel(s: PanelStatus): String = when (s) {
    PanelStatus.ONLINE -> "online"
    PanelStatus.DEGRADED -> "instável"
    PanelStatus.OFFLINE -> "offline"
    PanelStatus.USB -> "USB"
}
