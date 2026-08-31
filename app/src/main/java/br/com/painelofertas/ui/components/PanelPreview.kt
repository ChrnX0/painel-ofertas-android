package br.com.painelofertas.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas as GfxCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import br.com.painelofertas.render.PanelBitmap
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Desenha um [PanelBitmap] como uma placa de LED de verdade: cada ponto aceso é
 * um círculo com halo (bloom em camadas) e um brilho especular no topo; os pontos
 * apagados ficam tênues, formando a grade da matriz. É o "herói" da interface.
 *
 * ### Por que passa por um bitmap
 * Uma placa de 96×92 tem ~8.800 células, e cada acesa custa quatro círculos: são
 * até **35 mil operações de desenho**. Desenhar isso direto no `Canvas` significa
 * repetir tudo a cada quadro em que qualquer coisa por perto se mexe — o fôlego do
 * app, o arraste entre telas, a aurora. Foi o que saturou a *RenderThread* a ponto
 * de o Android acusar ANR: a thread principal ficava esperando o desenho terminar.
 *
 * Aqui a placa é rasterizada **uma vez** num [ImageBitmap] e depois só copiada.
 * `drawWithCache` refaz o bitmap quando o tamanho muda ou quando o conteúdo muda
 * (a lambda captura [bitmap] e as cores), mas **não** a cada quadro. As 35 mil
 * operações viram uma.
 */
@Composable
fun PanelPreview(
    bitmap: PanelBitmap,
    modifier: Modifier = Modifier,
    litColor: Color = Color(0xFFFFB300),
    offColor: Color = Color(0x12FFFFFF),
    showOff: Boolean = true,
    /** Inverte aceso/apagado — usado para mostrar o efeito "Pisca / Inverte". */
    inverted: Boolean = false,
) {
    Spacer(
        modifier.drawWithCache {
            val w = size.width.roundToInt()
            val h = size.height.roundToInt()
            if (w <= 0 || h <= 0 || bitmap.cols <= 0 || bitmap.rows <= 0) {
                return@drawWithCache onDrawBehind { }
            }
            val placa = rasterizar(bitmap, w, h, litColor, offColor, showOff, inverted)
            onDrawBehind {
                drawImage(
                    image = placa,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(w, h),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(w, h),
                )
            }
        },
    )
}

/** Desenha a matriz de LEDs num bitmap do tamanho exato do espaço disponível. */
private fun rasterizar(
    bitmap: PanelBitmap,
    w: Int,
    h: Int,
    litColor: Color,
    offColor: Color,
    showOff: Boolean,
    inverted: Boolean,
): ImageBitmap {
    val img = ImageBitmap(w, h)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, GfxCanvas(img), Size(w.toFloat(), h.toFloat())) {
        val cols = bitmap.cols
        val rows = bitmap.rows
        val cell = min(w.toFloat() / cols, h.toFloat() / rows)
        if (cell <= 0f) return@draw
        val offX = (w - cell * cols) / 2f
        val offY = (h - cell * rows) / 2f
        val rCore = cell * 0.40f
        val rOff = cell * 0.30f

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cx = offX + c * cell + cell / 2f
                val cy = offY + r * cell + cell / 2f
                val center = Offset(cx, cy)
                if (bitmap.isLit(r, c) != inverted) {
                    // halo externo + interno (bloom)
                    drawCircle(litColor.copy(alpha = 0.09f), radius = cell * 0.95f, center = center)
                    drawCircle(litColor.copy(alpha = 0.20f), radius = cell * 0.60f, center = center)
                    // núcleo aceso
                    drawCircle(litColor, radius = rCore, center = center)
                    // brilho especular (canto superior-esquerdo)
                    drawCircle(
                        Color.White.copy(alpha = 0.28f),
                        radius = rCore * 0.34f,
                        center = Offset(cx - rCore * 0.30f, cy - rCore * 0.30f),
                    )
                } else if (showOff) {
                    drawCircle(offColor, radius = rOff, center = center)
                }
            }
        }
    }
    return img
}
