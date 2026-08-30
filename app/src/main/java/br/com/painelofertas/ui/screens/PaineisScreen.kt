package br.com.painelofertas.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.draw.rotate
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
import br.com.painelofertas.ui.components.Accent
import br.com.painelofertas.ui.components.AccentOutlinedButton
import br.com.painelofertas.ui.components.Appear
import br.com.painelofertas.ui.components.ButtonShape
import br.com.painelofertas.ui.components.CardHeader
import br.com.painelofertas.ui.components.EmptyState
import br.com.painelofertas.ui.components.accentCardColors
import br.com.painelofertas.ui.components.MonoText
import br.com.painelofertas.ui.components.SectionLabel
import br.com.painelofertas.ui.LocalSnackbar
import br.com.painelofertas.ui.rememberContainer
import br.com.painelofertas.usb.WifiModuleConfigurator
import kotlinx.coroutines.launch

@Composable
fun PaineisScreen() {
    val container = rememberContainer()
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    val panels by container.panels.panels.collectAsState()
    val usbConnected by container.usb.connected.collectAsState()
    // IP do aparelho SEMPRE detectado agora (não usa mais valor salvo, que virava
    // velho e quebrava a busca). Recalcula ao (re)entrar na aba.
    val localIp = remember { container.currentLocalIp() }
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
                fun cmd(text: String) = scope.launch {
                    UdpLink(p.ip, container.udp).sendText(text)
                    snackbar.showSnackbar("Enviado ao painel: $text")
                }
                Appear(delayMillis = 60 + i * 40) {
                    PanelCard(
                        p,
                        usbConnected = usbConnected,
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
                        onSaveConfig = { note, grupo, ssid, senha, dhcp, sip, gw, mask ->
                            container.panels.setConfig(p.id, note, grupo, ssid, senha, dhcp, sip, gw, mask)
                        },
                        onApplyUsb = { ssid, senha, dhcp, sip, gw, mask ->
                            container.usb.link.value?.let { link ->
                                scope.launch {
                                    WifiModuleConfigurator(link).join(
                                        ssid = ssid, password = senha, localIp = localIp,
                                        dhcp = dhcp, staticIp = sip, gateway = gw, netmask = mask,
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        Appear { RedeAparelhoCard(localIp) }
        Appear(delayMillis = 40) { WifiConfigCard(usbConnected, localIp, container, scope) }
        Appear(delayMillis = 80) { PanelPasswordCard(usbConnected, container, scope) }
        Appear(delayMillis = 120) { DiagnosticoCard() }
    }
}

/** Diagnóstico do dispositivo/painel conectado (movido de Config). */
@Composable
private fun DiagnosticoCard() {
    val container = rememberContainer()
    val scope = rememberCoroutineScope()
    val usbConnected by container.usb.connected.collectAsState()
    val usbInfo by container.usb.info.collectAsState()
    val paineis by container.panels.panels.collectAsState()
    var probing by remember { mutableStateOf(false) }
    var probe by remember { mutableStateOf<WifiModuleConfigurator.DeviceProbe?>(null) }
    var probeMsg by remember { mutableStateOf("") }

    Card(Modifier.fillMaxWidth(), colors = accentCardColors(Accent.Lilac)) {
        Column(Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CardHeader(Icons.Filled.Memory, Accent.Lilac, "Diagnóstico do dispositivo")

            val info = usbInfo
            if (usbConnected && info != null) {
                Text("● Conectado por USB", style = MaterialTheme.typography.bodyMedium, color = Accent.Green)
                MonoText("chip: ${chipName(info.vendorId)}", size = 11)
                MonoText("VID 0x%04X · PID 0x%04X".format(info.vendorId, info.productId), size = 11)
                info.manufacturer?.takeIf { it.isNotBlank() }?.let { MonoText("fabricante: $it", size = 11) }
                info.product?.takeIf { it.isNotBlank() }?.let { MonoText("produto: $it", size = 11) }
                info.serial?.takeIf { it.isNotBlank() }?.let { MonoText("série: $it", size = 11) }
            } else {
                Text("Nenhum painel conectado por USB.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            OutlinedButton(onClick = { container.usb.ensureConnected() }, shape = ButtonShape) { Text("Procurar dispositivo USB") }

            if (usbConnected) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                OutlinedButton(
                    enabled = !probing,
                    shape = ButtonShape,
                    onClick = {
                        val link = container.usb.link.value ?: return@OutlinedButton
                        probing = true; probeMsg = "Consultando o módulo…"; probe = null
                        scope.launch {
                            val r = runCatching { WifiModuleConfigurator(link).probe() }.getOrNull()
                            probe = r
                            val vazio = r == null || (r.firmware.isEmpty() && r.mac.isBlank())
                            probeMsg = if (vazio) "Sem resposta do módulo (verifique o cabo/OTG)." else ""
                            // Guarda a identificação para consulta/suporte, mesmo desconectado.
                            if (!vazio && r != null) {
                                container.settings.firmwareInfo = buildString {
                                    if (r.mac.isNotBlank()) appendLine("MAC: ${r.mac}")
                                    if (r.ip.isNotBlank()) appendLine("IP: ${r.ip}")
                                    r.firmware.forEach { appendLine(it) }
                                }.trim()
                                container.settings.firmwareLidoEm = System.currentTimeMillis()
                            }
                            probing = false
                        }
                    },
                ) { Text(if (probing) "Consultando…" else "Ler firmware & MAC do módulo Wi-Fi") }

                probe?.let { r ->
                    if (r.mac.isNotBlank()) MonoText("MAC: ${r.mac}", size = 11, color = MaterialTheme.colorScheme.primary)
                    if (r.ip.isNotBlank()) MonoText("IP: ${r.ip}", size = 11)
                    r.firmware.forEach { MonoText(it, size = 11) }
                }
                if (probeMsg.isNotBlank()) {
                    Text(probeMsg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            paineis.firstOrNull()?.let { p ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                MonoText("memória livre (${p.name}): ${p.freeMemory} bytes", size = 11)
            }

            // Última identificação lida — fica guardada para consulta e suporte.
            val fwSalvo = container.settings.firmwareInfo
            if (fwSalvo.isNotBlank() && probe == null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SectionLabel("Última leitura do módulo")
                MonoText(fwSalvo, size = 10)
                MonoText("lido ${agoText(container.settings.firmwareLidoEm)}", size = 9, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (fwSalvo.isNotBlank() || probe != null) {
                Text(
                    "Atualização de firmware não é feita por aqui: o procedimento oficial é da LedBlock " +
                        "(fabricante). Informe estes dados ao suporte se precisar atualizar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            val logLines by container.diag.lines.collectAsState()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                SectionLabel("Registro (TX/RX)")
                TextButton(onClick = { container.diag.clear() }, enabled = logLines.isNotEmpty()) { Text("Limpar") }
            }
            if (logLines.isEmpty()) {
                MonoText(
                    "Sem tráfego ainda. Toque em Ligar/Identificar num painel e veja aqui o que sai (TX) e o que o painel responde (RX).",
                    size = 10, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                logLines.takeLast(15).forEach { l ->
                    MonoText("${l.time} ${l.dir} ${l.peer}  ${l.text}", size = 10, color = if (l.dir == "TX") Accent.Blue else Accent.Green)
                }
            }
        }
    }
}

/** Nome do fabricante a partir do VID USB (os mais comuns em pontes seriais/HID). */
private fun chipName(vid: Int): String = when (vid) {
    0x04D8 -> "Microchip (ponte USB / PIC)"
    0x10C4 -> "Silicon Labs (CP210x)"
    0x1A86 -> "WCH (CH340 / CH9102)"
    0x0403 -> "FTDI"
    else -> "VID 0x%04X".format(vid)
}

/** Rede do celular (movido de Config): IP detectado (informativo) + DHCP do painel. */
@Composable
private fun RedeAparelhoCard(localIp: String) {
    val container = rememberContainer()
    var dhcp by remember { mutableStateOf(container.settings.dhcp) }
    Card(Modifier.fillMaxWidth(), colors = accentCardColors(Accent.Blue)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CardHeader(Icons.Filled.Router, Accent.Blue, "Rede do aparelho", "Como o painel encontra este celular na rede.")
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text("IP deste celular · detectado automaticamente", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MonoText(localIp.ifBlank { "— (sem Wi-Fi)" }, size = 15, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Painel usa DHCP", style = MaterialTheme.typography.bodyLarge)
                Switch(dhcp, { dhcp = it; container.settings.dhcp = it })
            }
        }
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

    Card(Modifier.fillMaxWidth(), colors = accentCardColors(Accent.Amber)) {
        Column(Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CardHeader(Icons.Filled.Lock, Accent.Amber, "Senha do painel (via USB)")
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
    usbConnected: Boolean,
    onApply: (Int, Boolean) -> Unit,
    onIdentificar: () -> Unit,
    onLigar: () -> Unit,
    onDesligar: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onSaveConfig: (String, String, String, String, Boolean, String, String, String) -> Unit,
    onApplyUsb: (String, String, Boolean, String, String, String) -> Unit,
) {
    var nome by remember(p.id) { mutableStateOf(p.name) }
    var brilho by remember(p.id, p.brightness) { mutableFloatStateOf(p.brightness.toFloat()) }
    var sensor by remember(p.id, p.sensorAuto) { mutableStateOf(p.sensorAuto) }

    Card(Modifier.fillMaxWidth(), colors = accentCardColors(statusColor(p.status))) {
        Column(Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // status (chip colorido) + excluir
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                StatusChip(p.status)
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Remover da lista", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            OutlinedTextField(nome, { nome = it }, label = { Text("Nome do painel") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            SyncBadge(p.syncState)

            MonoText("IP ${p.ip} · livre ${p.freeMemory} B · CRC 0x%04X".format(p.crcPanel), size = 11)
            if (p.status != PanelStatus.ONLINE && p.lastSeen > 0) {
                MonoText("${statusLabel(p.status)} · visto ${agoText(p.lastSeen)}", size = 10, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

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
                AccentOutlinedButton(onClick = onLigar, accent = Accent.Green) { Text("Ligar") }
                AccentOutlinedButton(onClick = onDesligar, accent = Accent.Rose) { Text("Desligar") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccentOutlinedButton(onClick = onIdentificar, accent = Accent.Amber) { Text("Identificar") }
                AccentOutlinedButton(onClick = { onRename(nome) }, accent = Accent.Blue) { Text("Renomear") }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PanelConfigSection(p, usbConnected, onSaveConfig, onApplyUsb)
        }
    }
}

/** Seção expansível de config por painel: apelido + rede própria (Wi-Fi/DHCP/IP fixo). */
@Composable
private fun PanelConfigSection(
    p: Panel,
    usbConnected: Boolean,
    onSaveConfig: (String, String, String, String, Boolean, String, String, String) -> Unit,
    onApplyUsb: (String, String, Boolean, String, String, String) -> Unit,
) {
    var expanded by remember(p.id) { mutableStateOf(false) }
    var note by remember(p.id, p.note) { mutableStateOf(p.note) }
    var grupo by remember(p.id, p.group) { mutableStateOf(p.group) }
    var ssid by remember(p.id, p.ssid) { mutableStateOf(p.ssid) }
    var senha by remember(p.id, p.wifiPassword) { mutableStateOf(p.wifiPassword) }
    var dhcp by remember(p.id, p.dhcp) { mutableStateOf(p.dhcp) }
    var sip by remember(p.id, p.staticIp) { mutableStateOf(p.staticIp) }
    var gw by remember(p.id, p.gateway) { mutableStateOf(p.gateway) }
    var mask by remember(p.id, p.netmask) { mutableStateOf(p.netmask) }
    var saved by remember(p.id) { mutableStateOf(false) }

    Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SectionLabel("Configurações do painel")
            val rot by animateFloatAsState(if (expanded) 180f else 0f, label = "cfg")
            Icon(Icons.Filled.ExpandMore, if (expanded) "Recolher" else "Expandir", Modifier.rotate(rot))
        }
        androidx.compose.animation.AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(note, { note = it; saved = false }, label = { Text("Apelido / observação") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    grupo, { grupo = it; saved = false },
                    label = { Text("Grupo / setor (ex.: Açougue)") },
                    supportingText = { Text("Painéis do mesmo grupo recebem o envio juntos.") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Rede que este painel usa (aplicada por cabo USB).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(ssid, { ssid = it; saved = false }, label = { Text("Rede Wi-Fi (SSID)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(senha, { senha = it; saved = false }, label = { Text("Senha do Wi-Fi") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Usar DHCP (IP automático)", style = MaterialTheme.typography.bodyLarge)
                    Switch(dhcp, { dhcp = it; saved = false })
                }
                androidx.compose.animation.AnimatedVisibility(!dhcp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(sip, { sip = it; saved = false }, label = { Text("IP fixo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(gw, { gw = it; saved = false }, label = { Text("Gateway") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(mask, { mask = it; saved = false }, label = { Text("Máscara") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onSaveConfig(note, grupo, ssid, senha, dhcp, sip, gw, mask); saved = true }, shape = ButtonShape) { Text("Salvar") }
                    OutlinedButton(onClick = { onApplyUsb(ssid, senha, dhcp, sip, gw, mask) }, enabled = usbConnected, shape = ButtonShape) { Text("Aplicar via USB") }
                }
                if (saved) Text("✓ Salvo neste aparelho.", style = MaterialTheme.typography.bodySmall, color = Accent.Green)
            }
        }
    }
}

/** Chip de status colorido (online/instável/offline/USB), com bolinha que pulsa se online. */
@Composable
private fun StatusChip(status: PanelStatus) {
    val color = statusColor(status)
    val pulseAlpha by rememberInfiniteTransition(label = "live").animateFloat(
        1f, 0.4f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "liveDot",
    )
    val dotAlpha = if (status == PanelStatus.ONLINE) pulseAlpha else 1f
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .semantics { contentDescription = "Status: ${statusLabel(status)}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color.copy(alpha = dotAlpha)))
        Text(statusLabel(status), style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Medium)
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

    Card(Modifier.fillMaxWidth(), colors = accentCardColors(Accent.Teal)) {
        Column(Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CardHeader(Icons.Filled.Wifi, Accent.Teal, "Conectar painel novo (via USB)")
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
    PanelStatus.ONLINE -> Accent.Green
    PanelStatus.DEGRADED -> Accent.Amber
    PanelStatus.OFFLINE -> Accent.Gray
    PanelStatus.USB -> Accent.Blue
}

/** Texto relativo de "visto por último" (para o histórico de painéis). */
private fun agoText(lastSeen: Long): String {
    val diff = System.currentTimeMillis() - lastSeen
    if (diff < 60_000) return "agora há pouco"
    val min = diff / 60_000
    if (min < 60) return "há $min min"
    val h = min / 60
    if (h < 24) return "há $h h"
    return "há ${h / 24} d"
}

private fun statusLabel(s: PanelStatus): String = when (s) {
    PanelStatus.ONLINE -> "online"
    PanelStatus.DEGRADED -> "instável"
    PanelStatus.OFFLINE -> "offline"
    PanelStatus.USB -> "USB"
}
