package br.com.painelofertas.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import br.com.painelofertas.render.PanelBitmap
import kotlin.math.min

/**
 * Desenha um [PanelBitmap] como uma placa de LED de verdade: cada ponto aceso é
 * um círculo com halo (bloom em camadas) e um brilho especular no topo; os pontos
 * apagados ficam tênues, formando a grade da matriz. É o "herói" da interface.
 */
@Composable
fun PanelPreview(
    bitmap: PanelBitmap,
    modifier: Modifier = Modifier,
    litColor: Color = Color(0xFFFFB300),
    offColor: Color = Color(0x12FFFFFF),
    showOff: Boolean = true,
) {
    Canvas(modifier) {
        val cols = bitmap.cols
        val rows = bitmap.rows
        if (cols <= 0 || rows <= 0) return@Canvas

        val cell = min(size.width / cols, size.height / rows)
        if (cell <= 0f) return@Canvas
        val offX = (size.width - cell * cols) / 2f
        val offY = (size.height - cell * rows) / 2f
        val rCore = cell * 0.40f
        val rOff = cell * 0.30f

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cx = offX + c * cell + cell / 2f
                val cy = offY + r * cell + cell / 2f
                val center = Offset(cx, cy)
                if (bitmap.isLit(r, c)) {
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
}
