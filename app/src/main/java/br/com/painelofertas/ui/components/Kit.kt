package br.com.painelofertas.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import br.com.painelofertas.ui.theme.SquircleShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.painelofertas.net.LinkPhase
import br.com.painelofertas.ui.theme.Archivo
import br.com.painelofertas.ui.theme.LocalAccents
import br.com.painelofertas.ui.theme.LocalIsLight
import br.com.painelofertas.ui.theme.Mono
import br.com.painelofertas.ui.theme.PanelBg
import br.com.painelofertas.ui.theme.SquircleMedium
import br.com.painelofertas.ui.theme.SquirclePill

/** Cantos dos botões principais — superelípticos, estilo One UI. */
val ButtonShape = SquirclePill

/**
 * Acentos **pastéis** do app. Cada cor lê a paleta do tema atual
 * ([LocalAccents]), então o mesmo `Accent.Blue` sai claro no tema escuro e mais
 * fundo no claro — mesmo matiz, contraste sempre legível, sem dois nomes para a
 * mesma cor espalhados pelo código.
 */
object Accent {
    val Blue: Color @Composable @ReadOnlyComposable get() = LocalAccents.current.blue
    val Green: Color @Composable @ReadOnlyComposable get() = LocalAccents.current.green
    val Amber: Color @Composable @ReadOnlyComposable get() = LocalAccents.current.amber
    val Lilac: Color @Composable @ReadOnlyComposable get() = LocalAccents.current.lilac
    val Rose: Color @Composable @ReadOnlyComposable get() = LocalAccents.current.rose
    val Teal: Color @Composable @ReadOnlyComposable get() = LocalAccents.current.teal
    val Peach: Color @Composable @ReadOnlyComposable get() = LocalAccents.current.peach
    val Mint: Color @Composable @ReadOnlyComposable get() = LocalAccents.current.mint
    val Sky: Color @Composable @ReadOnlyComposable get() = LocalAccents.current.sky
    val Gray: Color @Composable @ReadOnlyComposable get() = LocalAccents.current.gray
}

/**
 * Texto/ícone para uso SOBRE um preenchimento de acento. No escuro o acento é
 * claro, então escreve-se em quase-preto; no claro o acento é fundo, e o texto
 * vira branco. Sem isso, botão pastel no tema claro fica ilegível.
 */
val OnAccent: Color
    @Composable @ReadOnlyComposable get() = if (LocalIsLight.current) Color.White else Color(0xFF0A0F16)

/**
 * Cartão com um **banho leve** da cor de acento — dá cor à superfície inteira,
 * porém bem suave (nada gritante). Combine com um [CardHeader] do mesmo tom.
 */
@Composable
fun accentCardColors(accent: Color): CardColors = accentCardColors(accent, 0.22f)

/**
 * [forca] é quanto da cor entra na superfície. 0.22 é o padrão: num fundo escuro
 * a mistura ainda deixa o texto em ~7:1 de contraste, e é o ponto onde a cor
 * finalmente **se lê** como cor, não como cinza sujo.
 */
@Composable
fun accentCardColors(accent: Color, forca: Float): CardColors {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    return CardDefaults.cardColors(containerColor = lerp(base, accent, forca))
}

/**
 * Fio de contorno no tom do cartão. A borda carrega cor em **área pequena**, então
 * pode ser bem mais saturada que o preenchimento sem atrapalhar a leitura — é o
 * que faz a cor parecer intencional em vez de um banho apagado.
 */
fun accentBorder(accent: Color) = BorderStroke(1.dp, accent.copy(alpha = 0.35f))

/**
 * Escala e opacidade que acompanham o fôlego do app. Aplique em cartões e blocos
 * grandes: cada um infla ~1,5% e clareia junto com todos os outros. Sozinho é
 * quase invisível; o efeito nasce de **tudo se mexer em sincronia**.
 *
 * A leitura do fôlego fica dentro da lambda do `graphicsLayer`, então só a fase de
 * desenho reexecuta — o layout nunca é remedido, e nada recompõe.
 */
@Composable
fun Modifier.breathe(intensidade: Float = 1f): Modifier {
    val breath = LocalBreath.current
    return graphicsLayer {
        val ar = breath.value
        val s = 1f + (ar - 0.5f) * 0.030f * intensidade
        scaleX = s
        scaleY = s
    }
}

/**
 * **Contorno que respira** — a versão do fôlego para cartões grandes.
 *
 * Escalar um cartão faria o texto dentro dele ser rerrasterizado a cada passo, e
 * o resultado é um tremeluzir feio nas letras. O contorno resolve isso: a
 * geometria fica parada, só a **luz** da borda vai e vem. Custa um traço de 1 px
 * por quadro, e como todos os cartões usam o mesmo fôlego, a tela inteira acende
 * e apaga junta.
 */
@Composable
fun Modifier.breathingBorder(
    accent: Color,
    shape: Shape,
    largura: Dp = 1.2.dp,
    min: Float = 0.16f,
    max: Float = 0.62f,
): Modifier {
    val breath = LocalBreath.current
    return drawWithContent {
        drawContent()
        drawOutline(
            outline = shape.createOutline(size, layoutDirection, this),
            color = accent.copy(alpha = breath.value.entre(min, max)),
            style = Stroke(largura.toPx()),
        )
    }
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
 * Botão de escolha com **desenho do que vai acontecer**. Duas ou três lado a lado
 * substituem um segmentado (ou um interruptor com rótulo abstrato): o lojista vê
 * a forma da tela em vez de ler "meia tela" e ter que imaginar.
 *
 * Ligado/desligado no mesmo idioma em toda a tela — é o que faz os passos
 * parecerem naturais em vez de um formulário.
 */
@Composable
fun OptionTile(
    selected: Boolean,
    title: String,
    hint: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    visual: @Composable () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val forma = SquircleShape(18.dp)
    val bg by animateColorAsState(
        if (selected) lerp(cs.surfaceContainerHigh, accent, if (LocalIsLight.current) 0.16f else 0.22f)
        else cs.surfaceContainerLow,
        Motion.gentle(), label = "tileBg",
    )
    val edge by animateColorAsState(
        if (selected) accent.copy(alpha = 0.75f) else cs.outlineVariant,
        Motion.gentle(), label = "tileEdge",
    )
    // Escolhido cresce um tiquinho — o par inteiro "respira" quando você troca.
    val escala by animateFloatAsState(if (selected) 1.035f else 0.94f, Motion.bouncy(), label = "tileScale")
    val press = remember { MutableInteractionSource() }

    Column(
        modifier
            .graphicsLayer { scaleX = escala; scaleY = escala }
            .pressBounce(press)
            .clip(forma)
            .background(bg)
            .border(if (selected) 1.5.dp else 1.dp, edge, forma)
            .clickable(interactionSource = press, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { visual() }
            // A marca de seleção fecha o par: um está ligado, o outro não.
            AnimatedContent(
                targetState = selected,
                transitionSpec = { scaleIn(Motion.bouncy()) + fadeIn() togetherWith scaleOut(Motion.gentle()) + fadeOut() },
                label = "tileCheck",
            ) { on ->
                Icon(
                    if (on) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    null,
                    Modifier.size(17.dp),
                    tint = if (on) accent else cs.outline,
                )
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) cs.onSurface else cs.onSurfaceVariant,
        )
        Text(hint, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
    }
}

/**
 * Miniatura da placa de LED: o retângulo escuro do painel com a área que a tela
 * vai ocupar acesa em [accent]. É o desenho usado nos [OptionTile] de formato.
 *
 * [fill] = fração da largura ocupada (1f painel inteiro, 0.5f meia tela);
 * [tall] = painel em pé.
 */
@Composable
fun PanelShape(fill: Float, tall: Boolean, accent: Color, on: Boolean) {
    val cs = MaterialTheme.colorScheme
    val w = if (tall) 26.dp else 46.dp
    val h = if (tall) 34.dp else 24.dp
    Box(
        Modifier.size(width = w, height = h).clip(RoundedCornerShape(4.dp)).background(PanelBg)
            .border(1.dp, cs.outlineVariant, RoundedCornerShape(4.dp))
            .padding(2.5.dp),
    ) {
        Box(
            Modifier.fillMaxWidth(fill).height(h - 5.dp).clip(RoundedCornerShape(2.dp))
                .background(if (on) accent.copy(alpha = 0.85f) else cs.outline.copy(alpha = 0.35f)),
        )
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
        // Quadradinho tingido + anel do mesmo tom: área pequena, cor cheia.
        Box(
            Modifier.size(38.dp).clip(SquircleShape(13.dp))
                .background(tint.copy(alpha = 0.24f))
                .border(1.dp, tint.copy(alpha = 0.45f), SquircleShape(13.dp)),
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

/**
 * Divisor que **some nas pontas**. Uma linha reta de ponta a ponta corta o cartão
 * em dois; um traço que nasce e morre em transparente apenas separa, sem fatiar —
 * é a diferença entre uma costura e um vinco.
 */
@Composable
fun SoftDivider(modifier: Modifier = Modifier, accent: Color? = null) {
    val cor = accent ?: MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.5f to cor.copy(alpha = 0.5f),
                    1f to Color.Transparent,
                ),
            ),
    )
}

/** Rótulo de seção (sobrancelha): mono, maiúsculo, espaçado — ar de instrumento. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, accent: Color? = null) {
    Text(
        text = text.uppercase(),
        fontFamily = Mono,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.6.sp,
        // Com acento a sobrancelha assume a cor da seção; sem ele, fica neutra.
        color = accent ?: MaterialTheme.colorScheme.onSurfaceVariant,
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
    /** Cor do LED — o halo que a placa emite. */
    glow: Color = Color(0xFFFFB300),
    content: @Composable BoxScope.() -> Unit,
) {
    val breath = LocalBreath.current
    val sprite = rememberGlowSprite()

    Box(
        modifier
            // Halo: a placa "vaza" luz para fora da moldura e o brilho pulsa com o
            // fôlego do app. É um sprite esticado — um blit, não um gradiente por
            // quadro — e a leitura do fôlego mora dentro do drawBehind, então só a
            // fase de desenho reexecuta.
            .drawBehind {
                val ar = breath.value
                val extra = size.minDimension * ar.entre(0.55f, 0.95f)
                val w = (size.width + extra).toInt()
                val h = (size.height + extra).toInt()
                drawImage(
                    image = sprite,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(sprite.width, sprite.height),
                    dstOffset = IntOffset(((size.width - w) / 2f).toInt(), ((size.height - h) / 2f).toInt()),
                    dstSize = IntSize(w, h),
                    colorFilter = ColorFilter.tint(glow.copy(alpha = ar.entre(0.10f, 0.26f))),
                    filterQuality = FilterQuality.Low,
                )
            }
            .clip(SquircleShape(24.dp))
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
                .clip(SquircleShape(13.dp))
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
            Modifier.size(60.dp).clip(SquircleShape(21.dp)).background(cs.surfaceContainerHigh),
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
