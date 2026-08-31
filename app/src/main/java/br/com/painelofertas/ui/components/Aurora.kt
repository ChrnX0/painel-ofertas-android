package br.com.painelofertas.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas as GfxCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import br.com.painelofertas.ui.theme.LocalIsLight
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Lado do bitmap da mancha. Pequeno de propósito — ele é sempre ampliado e borrado. */
private const val BLOB_PX = 96

/**
 * **Aurora**: manchas de luz pastel que derivam devagar atrás do conteúdo.
 *
 * É o que tira o app do "plano". Fundo de cor chapada é a coisa mais inorgânica
 * que existe — nada na natureza é uniforme. Aqui três manchas grandes passeiam em
 * trajetórias de períodos **primos entre si** (29 s, 37 s, 43 s): como não são
 * múltiplos, a combinação não se repete de forma perceptível e o fundo não parece
 * um GIF em laço.
 *
 * ### Por que via bitmap
 * A versão óbvia — `drawCircle(Brush.radialGradient(...))` a cada quadro — travou
 * o app: são ~8 milhões de pixels por mancha, cada um exigindo o cálculo do
 * gradiente, 60 vezes por segundo. Aqui o gradiente é rasterizado **uma única
 * vez** num bitmap de [BLOB_PX]², e cada quadro faz só três *blits* ampliados,
 * que o Skia resolve com amostragem bilinear. A ampliação de 96 px para a tela
 * inteira ainda borra o resultado — o que, para uma mancha difusa, é exatamente o
 * que se quer.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    colors: List<Color>,
) {
    val light = LocalIsLight.current
    val blob = remember { criarMancha() }

    // Relógio próprio, a 4 quadros por segundo. Ver comentário abaixo.
    var segundos by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(TICK_MS)
            segundos += TICK_MS / 1000f
        }
    }

    // No claro o pastel some sobre branco: precisa de mais opacidade que no escuro.
    val alpha = if (light) 0.34f else 0.24f

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val lado = (maxOf(w, h) * 0.72f).roundToInt()

        PERIODOS.forEachIndexed { i, periodo ->
            val f = TAU * (segundos % periodo) / periodo
            // Lissajous: x e y com frequências diferentes → trajetória aberta,
            // que não refaz o mesmo caminho de volta.
            val cx = w * (0.5f + 0.40f * cos(f + i * 2.1f))
            val cy = h * (0.36f + 0.30f * sin(f * 1.37f + i * 1.7f))
            drawImage(
                image = blob,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(BLOB_PX, BLOB_PX),
                dstOffset = IntOffset((cx - lado / 2f).roundToInt(), (cy - lado / 2f).roundToInt()),
                dstSize = IntSize(lado, lado),
                colorFilter = ColorFilter.tint(colors[i % colors.size].copy(alpha = alpha)),
                filterQuality = FilterQuality.Low,
            )
        }
    }
}

private const val TAU = (2 * Math.PI).toFloat()

/** Períodos em segundos — primos entre si, para a combinação não se repetir. */
private val PERIODOS = listOf(29f, 37f, 43f)

/**
 * Intervalo entre redesenhos, em ms.
 *
 * A taxa de atualização de uma animação deve casar com a **velocidade do
 * conteúdo**, não com a do monitor. `animateFloat` invalida a cada quadro — certo
 * para um botão sendo pressionado, absurdo para uma mancha que leva 29 segundos
 * para dar uma volta: a 60 fps ela anda 1 pixel por quadro, e redesenhar a tela
 * inteira para isso custou 97% de CPU no emulador.
 *
 * A 4 fps a mancha anda ~20 px por passo — invisível, porque ela é um borrão de
 * 1700 px com 24% de opacidade. Medido no emulador (software puro): a versão a
 * 60 fps custava +58 pontos de CPU; esta custa +20, com o mesmo resultado.
 */
private const val TICK_MS = 250L

/**
 * Mancha base: disco branco que some para transparente na borda. A cor real vem
 * depois, do `ColorFilter.tint` — assim um bitmap só serve às três manchas.
 */
private fun criarMancha(): ImageBitmap {
    val bmp = ImageBitmap(BLOB_PX, BLOB_PX)
    val tamanho = Size(BLOB_PX.toFloat(), BLOB_PX.toFloat())
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, GfxCanvas(bmp), tamanho) {
        val r = BLOB_PX / 2f
        drawCircle(
            brush = Brush.radialGradient(
                // Queda suave: sem parada intermediária a borda marca um anel.
                0f to Color.White,
                0.45f to Color.White.copy(alpha = 0.55f),
                1f to Color.Transparent,
                center = Offset(r, r),
                radius = r,
            ),
            radius = r,
            center = Offset(r, r),
        )
    }
    return bmp
}
