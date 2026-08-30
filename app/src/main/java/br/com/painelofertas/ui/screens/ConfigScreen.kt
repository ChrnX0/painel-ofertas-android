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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.painelofertas.BuildConfig
import br.com.painelofertas.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import br.com.painelofertas.protocol.BinaryCodec
import br.com.painelofertas.net.UdpLink
import br.com.painelofertas.render.PanelRenderer
import br.com.painelofertas.ui.components.Accent
import br.com.painelofertas.ui.components.Appear
import br.com.painelofertas.ui.components.CardHeader
import br.com.painelofertas.ui.components.EffectPicker
import br.com.painelofertas.ui.components.PanelEffect
import br.com.painelofertas.ui.components.accentCardColors
import br.com.painelofertas.ui.theme.ledColorAt
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
    val themeMode by container.settings.themeMode.collectAsState()
    val ledColor by container.settings.ledColor.collectAsState()
    val scroll = rememberScrollState()

    var efeito by remember { mutableIntStateOf(container.settings.effectMode) }
    var efeitoMsg by remember { mutableStateOf("") }
    val paineis by container.panels.panels.collectAsState()
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp)) {
        Appear {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SectionLabel("Ajustes")
                Text("Configurações", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        Appear(delayMillis = 50) {
            Card(Modifier.fillMaxWidth().padding(top = 8.dp), colors = accentCardColors(Accent.Lilac)) {
                Column(Modifier.padding(16.dp)) {
                    CardHeader(Icons.Filled.Palette, Accent.Lilac, "Tema do aplicativo", "Aparência do app no celular — não altera o painel.")
                    SegChoice(
                        listOf("Sistema", "Claro", "Escuro"),
                        themeMode,
                        Modifier.padding(top = 12.dp).fillMaxWidth(),
                    ) { container.settings.setThemeMode(it) }
                }
            }
        }

        Appear(delayMillis = 90) {
            Card(Modifier.fillMaxWidth().padding(top = 8.dp), colors = accentCardColors(Accent.Amber)) {
                Column(Modifier.padding(16.dp)) {
                    CardHeader(Icons.Filled.Lightbulb, Accent.Amber, "Prévia — cor do LED")
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(top = 10.dp),
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
        }

        Appear(delayMillis = 130) {
            Card(Modifier.fillMaxWidth().padding(top = 8.dp), colors = accentCardColors(Accent.Teal)) {
                Column(Modifier.padding(16.dp)) {
                    CardHeader(Icons.Filled.AutoAwesome, Accent.Teal, "Efeito das telas no painel", "Toque para ver como fica — vale para todas as telas.")

                    // Amostra usada nas miniaturas animadas (a palavra "OFERTA").
                    val amostra = remember {
                        PanelRenderer.render(
                            listOf(br.com.painelofertas.protocol.PanelRecord.Text(9, 1, 2, 1, "OFERTA")),
                            cols = 58, rows = 20, fonts = container.fonts,
                        )
                    }
                    EffectPicker(
                        amostra = amostra,
                        selecionado = efeito,
                        litColor = ledColorAt(ledColor),
                        modifier = Modifier.padding(top = 12.dp),
                    ) { novo ->
                        efeito = novo
                        container.settings.effectMode = novo
                        // Aplica JÁ nos painéis conhecidos (antes só valia no próximo STATUS).
                        scope.launch {
                            paineis.forEach { p -> runCatching { UdpLink(p.ip, container.udp).sendText("ONLINE=$novo") } }
                            if (paineis.isNotEmpty()) efeitoMsg = "Efeito aplicado em ${paineis.size} painel(is)."
                        }
                    }
                    androidx.compose.animation.AnimatedVisibility(efeitoMsg.isNotBlank()) {
                        MonoText(efeitoMsg, size = 11, color = Accent.Green, modifier = Modifier.padding(top = 8.dp))
                    }
                    Text(
                        PanelEffect.entries.firstOrNull { it.index == efeito }?.descricao ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        Appear(delayMillis = 170) { ProtocolCheckCard() }
        Appear(delayMillis = 210) { SobreCard() }
    }
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
