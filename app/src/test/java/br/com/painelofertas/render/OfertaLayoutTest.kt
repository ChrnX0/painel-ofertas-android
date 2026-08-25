package br.com.painelofertas.render

import br.com.painelofertas.protocol.FrameType
import br.com.painelofertas.protocol.PanelRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Valida a lógica de layout da Oferta (separação reais/centavos e flags). */
class OfertaLayoutTest {

    private val font = FlbFont.fromText(
        javaClass.getResourceAsStream("/7x4.flb")!!.readBytes().toString(Charsets.ISO_8859_1)
    )
    private val fonts = FontProvider { font } // mesma fonte para todos os códigos (só testamos a lógica)

    private fun texts(spec: OfertaSpec): List<String> =
        OfertaLayout.build(spec, fonts, halfScreen = true)
            .records.filterIsInstance<PanelRecord.Text>().map { it.text }

    @Test
    fun splits_reais_and_cents() {
        val t = texts(OfertaSpec(cabecalho = "", valor = "990", centavosReduzidos = true))
        assertTrue("reais '9'", t.contains("9"))
        assertTrue("centavos ',90'", t.contains(",90"))
    }

    @Test
    fun four_digits_is_twelve_thirty_four() {
        val t = texts(OfertaSpec(cabecalho = "", valor = "1234"))
        assertTrue(t.contains("12"))
        assertTrue(t.contains(",34"))
    }

    @Test
    fun cents_off_shows_only_reais() {
        val t = texts(OfertaSpec(cabecalho = "", valor = "990", centavosDesligados = true))
        assertTrue(t.contains("990"))
        assertTrue("sem vírgula", t.none { it.startsWith(",") })
    }

    @Test
    fun three_decimals() {
        val t = texts(OfertaSpec(cabecalho = "", valor = "12345", centavos3Casas = true))
        assertTrue(t.contains("12"))
        assertTrue(t.contains(",345"))
    }

    @Test
    fun header_carries_option_flags() {
        val f = OfertaLayout.build(
            OfertaSpec(valor = "990", centavos3Casas = true, centavosReduzidos = true, subtituloAtivo = true),
            fonts, halfScreen = true,
        )
        assertEquals(FrameType.OFERTA, f.type)
        assertTrue(f.f3) // subtítulo ativo
        assertTrue(f.f5) // 3 casas
        assertTrue(f.f6) // reduzidos
    }

    @Test
    fun has_underline_graphic() {
        val recs = OfertaLayout.build(OfertaSpec(valor = "990"), fonts, halfScreen = true).records
        assertTrue(recs.any { it is PanelRecord.Graphic })
    }
}
