package br.com.painelofertas.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.painelofertas.data.Panel
import br.com.painelofertas.data.PanelStatus
import br.com.painelofertas.data.SyncState
import br.com.painelofertas.net.LocalIp
import br.com.painelofertas.net.UdpLink
import br.com.painelofertas.ui.components.Appear
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
    val localIp by remember { mutableStateOf(container.settings.localIp.ifBlank { LocalIp.detect() ?: "" }) }
    val scanning by container.discovery.scanning.collectAsState()

    // Auto-conectar: ao abrir a aba, re-anuncia na rede (painéis na mesma rede aparecem sozinhos).
    LaunchedEffect(Unit) { container.autoConnect() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Appear {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        SectionLabel("Dispositivos")
                        Text("Painéis", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Button(onClick = { scope.launch { container.autoConnect() } }, enabled = !scanning, shape = ButtonShape) {
                        if (scanning) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.size(8.dp)); Text("Procurando")
                        } else {
                            Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Procurar")
                        }
                    }
                }
                // Deixa CLARO que este IP é do celular, não do painel (a confusão anterior).
                MonoText(
                    "meu aparelho: ${localIp.ifBlank { "—" }}" +
                        if (scanning) " · varrendo a rede…" else " · ${panels.size} painel(is) na rede",
                    size = 11,
                )
            }
        }

        if (panels.isEmpty()) {
            Appear(delayMillis = 60) {
                EmptyState(
                    icon = Icons.Filled.Tv,
                    title = if (scanning) "Procurando painéis…" else "Nenhum painel ainda",
                    subtitle = "O app varre a rede sozinho e reprocura a cada poucos segundos. O painel precisa estar ligado e na mesma rede Wi-Fi que o celular.",
                )
            }
        } else {
            panels.forEachIndexed { i, p ->
                fun cmd(text: String) = scope.launch { UdpLink(p.ip, container.udp).sendText(text) }
                Appear(delayMillis = 60 + i * 40) {
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
                        onDelete = { container.panels.remove(p.id) },
                    )
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        Appear { WifiConfigCard(usbConnected, localIp, container, scope) }
        Appear(delayMillis = 60) { PanelPasswordCard(usbConnected, container, scope) }
    }
}

/**
 * Grava/troca/desliga a senha de transmissão NO painel (via USB). Porte de
 * BitBtn10Click. ⚠️ Trocar a senha APAGA a memória do painel — por isso confirma.
 */
@Composable
private fun PanelPasswordCard(
    usbConnected: Boolean,
    container: br.com.painelofertas.AppContainer,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    var senha by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var confirmSet by remember { mutableStateOf(false) }
    var confirmOff by remember { mutableStateOf(false) }

    fun aplicar(valor: Long?) {
        val link = container.usb.link.value ?: return
        scope.launch {
            WifiModuleConfigurator(link).setPassword(valor)
            msg = if (valor == null) "✅ Senha removida (painel apagado)." else "✅ Senha definida (painel apagado)."
            senha = ""
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Senha do painel (via USB)", style = MaterialTheme.typography.titleMedium)
            if (!usbConnected) {
                Text(
                    "Conecte por USB para definir ou remover a senha de transmissão exigida ao enviar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "Define, troca ou remove a senha exigida para enviar ao painel. Trocar a senha apaga o conteúdo gravado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    senha, { senha = it.filter { c -> c.isDigit() }.take(9) },
                    label = { Text("Nova senha (numérica)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { confirmSet = true }, enabled = senha.isNotBlank(), shape = ButtonShape) { Text("Definir senha") }
                    OutlinedButton(
                        onClick = { confirmOff = true }, shape = ButtonShape,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Remover") }
                }
                if (msg.isNotBlank()) Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (confirmSet) {
        AlertDialog(
            onDismissRequest = { confirmSet = false },
            title = { Text("Definir senha do painel?") },
            text = { Text("O painel passará a exigir esta senha para receber envios, e o conteúdo atual será apagado.") },
            confirmButton = { TextButton(onClick = { confirmSet = false; aplicar(senha.toLongOrNull()) }) { Text("Definir") } },
            dismissButton = { TextButton(onClick = { confirmSet = false }) { Text("Cancelar") } },
        )
    }
    if (confirmOff) {
        AlertDialog(
            onDismissRequest = { confirmOff = false },
            title = { Text("Remover a senha?") },
            text = { Text("O painel deixará de exigir senha, e o conteúdo atual será apagado.") },
            confirmButton = { TextButton(onClick = { confirmOff = false; aplicar(null) }) { Text("Remover") } },
            dismissButton = { TextButton(onClick = { confirmOff = false }) { Text("Cancelar") } },
        )
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
    onDelete: () -> Unit,
) {
    var nome by remember(p.id) { mutableStateOf(p.name) }
    var brilho by remember(p.id, p.brightness) { mutableFloatStateOf(p.brightness.toFloat()) }
    var sensor by remember(p.id, p.sensorAuto) { mutableStateOf(p.sensorAuto) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // status + nome + excluir
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // pulso só é "visível" quando online; a animação é sempre criada
                // (chamadas de composição não podem ser condicionais).
                val pulseAlpha by rememberInfiniteTransition(label = "live").animateFloat(
                    1f, 0.4f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "liveDot",
                )
                val dotAlpha = if (p.status == PanelStatus.ONLINE) pulseAlpha else 1f
                Box(
                    Modifier.size(11.dp).clip(CircleShape).background(statusColor(p.status).copy(alpha = dotAlpha))
                        .semantics { contentDescription = "Status: ${statusLabel(p.status)}" },
                )
                OutlinedTextField(nome, { nome = it }, label = { Text("Nome do painel") }, singleLine = true, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Remover da lista", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
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

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Wi-Fi do painel (via USB)", style = MaterialTheme.typography.titleMedium)
            if (!usbConnected) {
                Text(
                    "Conecte o painel por cabo USB (OTG) para configurar em qual rede Wi-Fi ele entra.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
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
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ssids.forEach { s ->
                            FilterChip(selected = ssidSel == s, onClick = { ssidSel = s }, label = { Text(s) })
                        }
                    }
                }
                OutlinedTextField(senha, { senha = it }, label = { Text("Senha da rede") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
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
                ) { Text("Entrar na rede") }
                if (msg.isNotBlank()) Text(msg, style = MaterialTheme.typography.bodySmall)
            }
        }
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
