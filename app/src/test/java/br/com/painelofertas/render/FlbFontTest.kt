package br.com.painelofertas.render

import br.com.painelofertas.protocol.PanelRecord
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Valida o parser/render de fontes `.flb` contra a fonte real 7x4.flb
 * (em src/test/resources). Os valores-ouro vieram da referência em Python.
 */
class FlbFontTest {

    private lateinit var font: FlbFont

    @Before
    fun setUp() {
        val text = javaClass.getResourceAsStream("/7x4.flb")!!
            .readBytes().toString(Charsets.ISO_8859_1)
        font = FlbFont.fromText(text)
    }

    @Test
    fun glyph_T_metrics_and_data() {
        val g = font.glyph('T')
        assertEquals(7, g.height)
        assertEquals(3, g.width)
        assertEquals(1, g.spacing)
        assertArrayEquals(intArrayOf(7, 2, 2, 2, 2, 2, 2), g.data)
        // linha 0 = 0b111 (barra do topo), linhas 1..6 = 0b010 (haste central)
        assertTrue(g.isLit(0, 0) && g.isLit(0, 1) && g.isLit(0, 2))
        assertTrue(!g.isLit(1, 0) && g.isLit(1, 1) && !g.isLit(1, 2))
    }

    @Test
    fun glyph_E_metrics() {
        val g = font.glyph('E')
        assertEquals(7, g.height)
        assertEquals(4, g.width)
        assertArrayEquals(intArrayOf(15, 1, 1, 7, 1, 1, 15), g.data)
    }

    @Test
    fun space_is_blank_width_3() {
        val g = font.glyph(' ')
        assertEquals(3, g.width)
        assertEquals(0, g.data.sum())
        assertEquals(4, g.advance) // 3 + 1 de espaçamento
    }

    @Test
    fun render_TE_into_bitmap() {
        val rec = PanelRecord.Text(slot = 9, row = 0, col = 0, font = 0, text = "TE")
        val bmp = PanelRenderer.render(listOf(rec), cols = 20, rows = 8) { font }
        // topo do 'T' (cols 0..2) aceso
        assertTrue(bmp.isLit(0, 0) && bmp.isLit(0, 1) && bmp.isLit(0, 2))
        // 'E' começa em col = 3(W do T) + 1(espaço) = 4; topo do E (cols 4..7) aceso
        assertTrue(bmp.isLit(0, 4) && bmp.isLit(0, 7))
        assertTrue(bmp.litCount > 0)
    }

    @Test
    fun measure_TESTE() {
        // T(3+1) E(4+1) S(?+1) T(3+1) E(4+1) — só checa que é positivo e consistente
        val w = font.measure("TESTE")
        assertTrue(w > 15)
    }
}
