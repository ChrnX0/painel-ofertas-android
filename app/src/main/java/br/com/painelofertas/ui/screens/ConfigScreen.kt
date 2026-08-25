package br.com.painelofertas.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import br.com.painelofertas.ui.theme.LedColorNames
import br.com.painelofertas.ui.theme.LedColors
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.painelofertas.BuildConfig
import br.com.painelofertas.R
import br.com.painelofertas.protocol.BinaryCodec
import br.com.painelofertas.ui.components.ButtonShape
import br.com.painelofertas.ui.components.MonoText
import br.com.painelofertas.ui.components.SectionLabel
import br.com.painelofertas.ui.components.SegChoice
import br.com.painelofertas.ui.rememberContainer
import br.com.painelofertas.usb.WifiModuleConfigurator
import kotlinx.coroutines.launch

private val EFEITOS = listOf("Padrão", "Pisca / Inverte", "Pisca / Padrão")

@Composable
fun ConfigScreen() {
    val container = rememberContainer()
    val usbConnected by container.usb.connected.collectAsState()
    val usbInfo by container.usb.info.collectAsState()
    val paineis by container.panels.panels.collectAsState()
    val themeMode by container.settings.themeMode.collectAsState()
    val ledColor by container.settings.ledColor.collectAsState()
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    var probing by remember { mutableStateOf(false) }
    var probe by remember { mutableStateOf<WifiModuleConfigurator.DeviceProbe?>(null) }
    var probeMsg by remember { mutableStateOf("") }

    var efeito by remember { mutableIntStateOf(container.settings.effectMode) }
    var localIp by remember { mutableStateOf(container.settings.localIp) }
    var dhcp by remember { mutableStateOf(container.settings.dhcp) }
    var usarSenha by remember { mutableStateOf(container.settings.useTxPassword) }
    var senha by remember { mutableStateOf(container.settings.txPassword) }

    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SectionLabel("Ajustes")
            Text("Configurações", style = MaterialTheme.typography.headlineSmall)
        }
        Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Tema", style = MaterialTheme.typography.titleMedium)
                SegChoice(
                    listOf("Sistema", "Claro", "Escuro"),
                    themeMode,
                    Modifier.padding(top = 8.dp).fillMaxWidth(),
                ) { container.settings.setThemeMode(it) }
            }
        }

        Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Prévia — cor do LED", style = MaterialTheme.typography.titleMedium)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    LedColors.forEachIndexed { i, c ->
                        FilterChip(
                            selected = ledColor == i,
                            onClick = { container.settings.setLedColor(i) },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(Modifier.size(13.dp).clip(CircleShape).background(c))
                                    Text(LedColorNames[i])
                                }
                            },
                        )
                    }
                }
                Text(
                    "Deixe igual à cor do seu painel real — a prévia imita o que aparece nele.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Efeito global das telas", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EFEITOS.forEachIndexed { i, nome ->
                        FilterChip(
                            selected = efeito == i,
                            onClick = { efeito = i; container.settings.effectMode = i },
                            label = { Text(nome, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                OutlinedTextField(
                    value = localIp,
                    onValueChange = { localIp = it; container.settings.localIp = it },
                    label = { Text("IP local do aparelho") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Usar DHCP no painel")
                    Switch(checked = dhcp, onCheckedChange = { dhcp = it; container.settings.dhcp = it })
                }
            }
        }

        Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Segurança de transmissão", style = MaterialTheme.typography.titleMedium)
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Enviar com senha")
                    Switch(checked = usarSenha, onCheckedChange = { usarSenha = it; container.settings.useTxPassword = it })
                }
                if (usarSenha) {
                    OutlinedTextField(
                        value = senha,
                        onValueChange = { senha = it; container.settings.txPassword = it },
                        label = { Text("Senha (numérica)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Diagnóstico do dispositivo", style = MaterialTheme.typography.titleMedium)

                val info = usbInfo
                if (usbConnected && info != null) {
                    Text("● Conectado por USB", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF34D399))
                    MonoText("chip: ${chipName(info.vendorId)}", size = 11)
                    MonoText("VID 0x%04X · PID 0x%04X".format(info.vendorId, info.productId), size = 11)
                    info.manufacturer?.takeIf { it.isNotBlank() }?.let { MonoText("fabricante: $it", size = 11) }
                    info.product?.takeIf { it.isNotBlank() }?.let { MonoText("produto: $it", size = 11) }
                    info.serial?.takeIf { it.isNotBlank() }?.let { MonoText("série: $it", size = 11) }
                } else {
                    Text("Nenhum painel conectado por USB.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Button(onClick = { container.usb.ensureConnected() }, shape = ButtonShape) {
                    Text("Procurar dispositivo USB")
                }

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
                                probeMsg = if (r == null || (r.firmware.isEmpty() && r.mac.isBlank()))
                                    "Sem resposta do módulo (verifique o cabo/OTG)." else ""
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
            }
        }

        ProtocolCheckCard()
        SobreCard()
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

/** Tela "Sobre" com a marca da LedBlock (fabricante do painel). */
@Composable
private fun SobreCard() {
    Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Painel de Ofertas",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("Versão ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
            Text(
                "Todos os direitos reservados a LedBlock Indicadores Inteligentes",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                "www.ledblock.com.br",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** Autoteste do protocolo (compila os arquivos reais no aparelho). */
@Composable
private fun ProtocolCheckCard() {
    val resultado = remember {
        val nelPai = listOf(
            ":1;1;0;0;0;0;1;1;", ";7;4;42;8;3;12", ";6;4;35;65;2;,",
            ";7;4;42;72;1;34", ";5;0;61;72;61;89;", ";10;1;5;37;0;TESTE", ";10;2;16;23;1;TESTE",
        )
        val r = BinaryCodec.compile(listOf("Painel 1", "0", "0", "100") + nelPai)
        val ok = r.consumo == 49 && r.crc == 0xD644 && BinaryCodec.decompile(r.bytes) == nelPai
        Triple(ok, r.consumo, r.crc)
    }
    Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text("Diagnóstico do protocolo", style = MaterialTheme.typography.titleMedium)
            Text(
                (if (resultado.first) "✅ OK" else "❌ FALHOU") +
                    " — ${resultado.second} bytes, CRC 0x%04X".format(resultado.third),
                color = if (resultado.first) Color(0xFF2E7D32) else Color(0xFFC62828),
            )
            Text("Compila nelPai.dll no aparelho e compara com o app Windows.",
                fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp))
        }
    }
}
