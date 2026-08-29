package br.com.painelofertas.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.painelofertas.net.LinkPhase
import br.com.painelofertas.ui.theme.Archivo
import br.com.painelofertas.ui.theme.Mono
import br.com.painelofertas.ui.theme.PanelBg

/** Cantos dos botões principais — arredondados, estilo OneUI. */
val ButtonShape = RoundedCornerShape(22.dp)

/**
 * Acentos **pastéis** — coloridos, porém suaves (nada gritante), legíveis nos dois
 * temas. Usados nos ícones dos cartões e nos chips de status para dar cor sem berrar.
 */
object Accent {
    val Blue = Color(0xFF8AB4F8)
    val Green = Color(0xFF8FE0BF)
    val Amber = Color(0xFFF3D08A)
    val Lilac = Color(0xFFC3B0F5)
    val Rose = Color(0xFFF2AEC6)
    val Teal = Color(0xFF8FD9D2)
    val Gray = Color(0xFF9AA6B6)
}

/** Texto/ícone escuro para uso SOBRE um preenchimento pastel (bom contraste). */
val OnAccent = Color(0xFF0A0F16)

/**
 * Cartão com um **banho leve** da cor de acento — dá cor à superfície inteira,
 * porém bem suave (nada gritante). Combine com um [CardHeader] do mesmo tom.
 */
@Composable
fun accentCardColors(accent: Color): CardColors {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    return CardDefaults.cardColors(containerColor = lerp(base, accent, 0.10f))
}

/** Botão preenchido em cor **pastel** (texto escuro para contraste). */
@Composable
fun AccentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick, modifier, enabled = enabled, shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = OnAccent),
        content = content,
    )
}

/** Botão contornado em cor pastel (borda + texto no tom do acento). */
@Composable
fun AccentOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick, modifier, enabled = enabled, shape = ButtonShape,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
        border = BorderStroke(1.5.dp, accent.copy(alpha = if (enabled) 0.5f else 0.2f)),
        content = content,
    )
}

/**
 * Wordmark vetorial da LedBlock — nítido em qualquer tamanho e adapta ao tema
 * ("LED" no azul da marca, "BL[bloco]CK" na cor do texto). O "O" de BLOCK é o
 * quadradinho (o "block"). Substitui o logo raster que ficava pequeno.
 */
@Composable
fun LedBlockWordmark(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("LED", fontFamily = Archivo, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, letterSpacing = (-0.5).sp, color = cs.primary)
        Text("BL", fontFamily = Archivo, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, letterSpacing = (-0.5).sp, color = cs.onSurface)
        Box(
            Modifier
                .padding(horizontal = 1.5.dp)
                .size(width = 13.dp, height = 14.dp)
                .border(2.4.dp, cs.onSurface, RoundedCornerShape(3.dp)),
        )
        Text("CK", fontFamily = Archivo, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, letterSpacing = (-0.5).sp, color = cs.onSurface)
    }
}

/** Marca compacta LedBlock (mini-grade de LEDs) — para o canto da barra superior. */
@Composable
fun LedBlockMark(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val pattern = listOf(
        listOf(true, false, true),
        listOf(true, true, false),
        listOf(false, true, true),
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        pattern.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { on ->
                    Box(
                        Modifier.size(5.dp).clip(RoundedCornerShape(1.5.dp))
                            .background(if (on) cs.primary else cs.surfaceContainerHighest),
                    )
                }
            }
        }
    }
}

/** Controle segmentado (escolha única) — troca os chips soltos por algo coeso. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegChoice(
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    SingleChoiceSegmentedButtonRow(modifier) {
        options.forEachIndexed { i, label ->
            SegmentedButton(
                selected = selectedIndex == i,
                onClick = { onSelect(i) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = cs.secondaryContainer,
                    activeContentColor = cs.onSecondaryContainer,
                    inactiveContainerColor = cs.surface,
                    inactiveContentColor = cs.onSurfaceVariant,
                    activeBorderColor = cs.primary,
                    inactiveBorderColor = cs.outlineVariant,
                ),
                label = { Text(label) },
            )
        }
    }
}

/**
 * Cabeçalho de cartão com **ícone colorido** num quadradinho tingido — injeta cor
 * e hierarquia visual (estilo One UI). O [tint] dá a identidade do cartão
 * (azul=rede, âmbar=segurança, verde=painel, etc.).
 */
@Composable
fun CardHeader(icon: ImageVector, tint: Color, title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(21.dp)) }
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Rótulo de seção (sobrancelha): mono, maiúsculo, espaçado — ar de instrumento. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        fontFamily = Mono,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.6.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 2.dp),
    )
}

/** Texto técnico monoespaçado (IP, %, versão, CRC). */
@Composable
fun MonoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Int = 12,
    weight: FontWeight = FontWeight.Medium,
) {
    Text(text, modifier = modifier, fontFamily = Mono, fontSize = size.sp, fontWeight = weight, color = color)
}

/**
 * A prévia dentro da moldura do equipamento — o herói. Fundo preto (a placa é
 * hardware físico, independe do tema), moldura de plástico escuro com parafusos
 * e a etiqueta "LEDBLOCK". Passe a [PanelPreview] como conteúdo.
 */
@Composable
fun LedBezel(
    modifier: Modifier = Modifier,
    boardHeight: Dp = 150.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF2A2F38), Color(0xFF141821), Color(0xFF1E232C)),
                ),
            )
            .padding(12.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(boardHeight)
                .clip(RoundedCornerShape(11.dp))
                .background(PanelBg),
            contentAlignment = Alignment.Center,
            content = content,
        )
        Text(
            "LEDBLOCK",
            fontFamily = Mono,
            fontSize = 8.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF5A6472),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 4.dp),
        )
        Screw(Modifier.align(Alignment.TopStart))
        Screw(Modifier.align(Alignment.BottomStart))
        Screw(Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun Screw(modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(3.dp)
            .size(5.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(listOf(Color(0xFF3B424D), Color(0xFF11151B))),
            ),
    )
}

/**
 * Pílula de status de um enlace (barra superior). A cor da bolinha traduz a
 * [LinkPhase] — verde online, amarelo procurando, azul transferindo, vermelho
 * erro, cinza desligado — com pulso lento ao procurar e piscada rápida ao
 * transferir. A cor muda com transição suave (nada "pula").
 */
@Composable
fun StatusPill(phase: LinkPhase, icon: ImageVector, contentDescription: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val target = when (phase) {
        LinkPhase.ONLINE -> Accent.Green
        LinkPhase.SEARCHING -> Accent.Amber
        LinkPhase.TRANSFER -> Accent.Blue
        LinkPhase.ERROR -> Accent.Rose
        LinkPhase.OFFLINE -> cs.outline
    }
    val dotColor by animateColorAsState(target, tween(400), label = "pillColor")

    val blinking = phase == LinkPhase.SEARCHING || phase == LinkPhase.TRANSFER
    val transition = rememberInfiniteTransition(label = "pill")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            tween(if (phase == LinkPhase.TRANSFER) 480 else 1300),
            RepeatMode.Reverse,
        ),
        label = "pillDot",
    )
    // halo suave quando ativo, para a bolinha "respirar"
    val glow = if (phase == LinkPhase.ONLINE) 0.5f else if (blinking) pulse * 0.6f else 0f

    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(cs.surfaceContainerHigh)
            .border(1.dp, cs.outlineVariant, RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (glow > 0f) {
                Box(Modifier.size(13.dp).clip(CircleShape).background(dotColor.copy(alpha = glow * 0.35f)))
            }
            Box(
                Modifier.size(7.dp).clip(CircleShape)
                    .background(dotColor.copy(alpha = if (blinking) pulse else 1f)),
            )
        }
        Icon(icon, contentDescription, tint = cs.onSurfaceVariant, modifier = Modifier.size(15.dp))
    }
}

/** Estado vazio elegante (mata o "oceano de preto" das telas sem dados). */
@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(cs.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = cs.primary, modifier = Modifier.size(30.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = cs.onSurface,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp).widthIn(max = 320.dp),
        )
    }
}
