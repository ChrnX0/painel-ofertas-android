package br.com.painelofertas.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Canto **superelíptico** (o "squircle") — a forma do One UI e do iOS.
 *
 * Um `RoundedCornerShape` é um arco de círculo colado numa reta: no ponto da
 * emenda a curvatura salta de zero para o máximo, e o olho enxerga isso como
 * dois pedaços grudados. A superelipse `|x/r|ⁿ + |y/r|ⁿ = 1` espalha a curvatura
 * pelo canto inteiro — começa a virar antes e termina depois, sem emenda. É o que
 * faz uma tela parecer **desenhada** em vez de montada.
 *
 * [n] controla o quanto: 2 = círculo (idêntico ao arredondado comum), 4–5 =
 * squircle, ∞ = quadrado. Usamos ~4,5, perto do que a Samsung emprega.
 *
 * Herda de [CornerBasedShape] (e não do `Shape` cru) porque é isso que o Material
 * exige em `MaterialTheme.shapes`: componentes **adaptam** a forma recebida — a
 * folha inferior pega `shapes.large` e zera os cantos de baixo. Um `Shape` simples
 * não teria como ser adaptado.
 */
class SquircleShape(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
    private val n: Float = 4.5f,
) : CornerBasedShape(topStart, topEnd, bottomEnd, bottomStart) {

    constructor(radius: Dp, n: Float = 4.5f) : this(
        CornerSize(radius), CornerSize(radius), CornerSize(radius), CornerSize(radius), n,
    )

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ) = SquircleShape(topStart, topEnd, bottomEnd, bottomStart, n)

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        if (topStart + topEnd + bottomEnd + bottomStart == 0f) {
            return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        }
        // "start/end" viram "left/right" conforme a direção de leitura.
        val ltr = layoutDirection == LayoutDirection.Ltr
        val tl = if (ltr) topStart else topEnd
        val tr = if (ltr) topEnd else topStart
        val br = if (ltr) bottomEnd else bottomStart
        val bl = if (ltr) bottomStart else bottomEnd
        return Outline.Generic(squirclePath(size.width, size.height, tl, tr, br, bl, n))
    }

    override fun toString() = "SquircleShape(n=$n)"
}

private const val SEGMENTS = 14
private val HALF_PI = (Math.PI / 2).toFloat()

/**
 * Ponto do quadrante da superelipse em `t ∈ [0, π/2]`, com o centro do canto na
 * origem: em t=0 sai na horizontal, em t=π/2 na vertical.
 */
private fun sx(t: Float, r: Float, n: Float) = r * abs(cos(t)).pow(2f / n)
private fun sy(t: Float, r: Float, n: Float) = r * abs(sin(t)).pow(2f / n)

private fun squirclePath(
    w: Float,
    h: Float,
    tl: Float,
    tr: Float,
    br: Float,
    bl: Float,
    n: Float,
): Path = Path().apply {
    moveTo(tl, 0f)
    lineTo(w - tr, 0f)
    // canto superior direito — centro (w-tr, tr); percorre da borda de cima para a direita
    for (i in 0..SEGMENTS) {
        val t = HALF_PI - HALF_PI * i / SEGMENTS
        lineTo((w - tr) + sx(t, tr, n), tr - sy(t, tr, n))
    }
    lineTo(w, h - br)
    // inferior direito — centro (w-br, h-br)
    for (i in 0..SEGMENTS) {
        val t = HALF_PI * i / SEGMENTS
        lineTo((w - br) + sx(t, br, n), (h - br) + sy(t, br, n))
    }
    lineTo(bl, h)
    // inferior esquerdo — centro (bl, h-bl)
    for (i in 0..SEGMENTS) {
        val t = HALF_PI - HALF_PI * i / SEGMENTS
        lineTo(bl - sx(t, bl, n), (h - bl) + sy(t, bl, n))
    }
    lineTo(0f, tl)
    // superior esquerdo — centro (tl, tl)
    for (i in 0..SEGMENTS) {
        val t = HALF_PI * i / SEGMENTS
        lineTo(tl - sx(t, tl, n), tl - sy(t, tl, n))
    }
    close()
}

// ===== Escala de formas do app =====
val SquircleSmall = SquircleShape(14.dp)
val SquircleMedium = SquircleShape(22.dp)
val SquircleLarge = SquircleShape(28.dp)
val SquircleXLarge = SquircleShape(36.dp)

/** Botões e pílulas: canto bem generoso, ainda superelíptico. */
val SquirclePill = SquircleShape(24.dp, n = 5.5f)
