package br.com.painelofertas.render

import br.com.painelofertas.protocol.AccentMap
import br.com.painelofertas.protocol.PanelFrame
import br.com.painelofertas.protocol.PanelRecord

/** Grade de pixels do painel (aceso/apagado). Origem (0,0) no canto superior esquerdo. */
class PanelBitmap(val cols: Int, val rows: Int) {
    private val pixels = BooleanArray(cols.coerceAtLeast(0) * rows.coerceAtLeast(0))

    fun isLit(row: Int, col: Int): Boolean =
        row in 0 until rows && col in 0 until cols && pixels[row * cols + col]

    fun set(row: Int, col: Int) {
        if (row in 0 until rows && col in 0 until cols) pixels[row * cols + col] = true
    }

    val litCount: Int get() = pixels.count { it }
}

/** Fornecedor de fontes por código (0..4 -> as 5 fontes `.flb`). */
fun interface FontProvider {
    fun font(code: Int): FlbFont
}

/**
 * Renderiza registros de um quadro em uma grade de pixels — porte da parte de
 * desenho de `imprimir_tela` (Ofertas.pas). A centralização/layout que constrói
 * as posições fica no editor (Fase 3); aqui apenas desenhamos onde já está.
 */
object PanelRenderer {

    fun render(records: List<PanelRecord>, cols: Int, rows: Int, fonts: FontProvider): PanelBitmap {
        val bmp = PanelBitmap(cols, rows)
        for (rec in records) {
            when (rec) {
                is PanelRecord.Text -> drawText(bmp, rec, fonts)
                is PanelRecord.Graphic -> drawGraphic(bmp, rec)
            }
        }
        return bmp
    }

    fun renderFrame(frame: PanelFrame, cols: Int, rows: Int, fonts: FontProvider): PanelBitmap =
        render(frame.records, cols, rows, fonts)

    /** (maxCol, maxRow) ocupados pelo conteúdo — útil para dimensionar o preview. */
    fun contentBounds(records: List<PanelRecord>, fonts: FontProvider): Pair<Int, Int> {
        var maxCol = 0
        var maxRow = 0
        for (rec in records) {
            when (rec) {
                is PanelRecord.Text -> {
                    val font = fonts.font(rec.font)
                    val w = font.measure(AccentMap.normalize(rec.text))
                    val h = AccentMap.normalize(rec.text).maxOfOrNull { font.glyph(it).height } ?: 0
                    maxCol = maxOf(maxCol, rec.col + w)
                    maxRow = maxOf(maxRow, rec.row + h)
                }
                is PanelRecord.Graphic -> {
                    maxCol = maxOf(maxCol, rec.col1, rec.col2)
                    maxRow = maxOf(maxRow, rec.row1, rec.row2)
                }
            }
        }
        return maxCol to maxRow
    }

    private fun drawText(bmp: PanelBitmap, rec: PanelRecord.Text, fonts: FontProvider) {
        val font = fonts.font(rec.font)
        val text = AccentMap.normalize(rec.text)
        var x = rec.col
        for (c in text) {
            val g = font.glyph(c)
            for (gr in 0 until g.height) {
                for (gc in 0 until g.width) {
                    if (g.isLit(gr, gc)) bmp.set(rec.row + gr, x + gc)
                }
            }
            x += g.advance
        }
    }

    private fun drawGraphic(bmp: PanelBitmap, rec: PanelRecord.Graphic) {
        val r0 = minOf(rec.row1, rec.row2)
        val r1 = maxOf(rec.row1, rec.row2)
        val c0 = minOf(rec.col1, rec.col2)
        val c1 = maxOf(rec.col1, rec.col2)
        for (r in r0..r1) for (c in c0..c1) bmp.set(r, c)
    }
}
