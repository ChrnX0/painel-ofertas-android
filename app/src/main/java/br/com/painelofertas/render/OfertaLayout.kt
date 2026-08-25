package br.com.painelofertas.render

import br.com.painelofertas.protocol.AccentMap
import br.com.painelofertas.protocol.FrameType
import br.com.painelofertas.protocol.PanelFrame
import br.com.painelofertas.protocol.PanelRecord

/** Parâmetros de uma oferta de preço (equivale aos campos da aba Ofertas). */
data class OfertaSpec(
    val cabecalho: String = "",
    val titulo: String = "",
    val subtitulo: String = "",
    val valor: String = "",       // dígitos: "1234" -> 12,34
    val medida: String = "",
    val auxiliar: String = "",
    val rodape: String = "",
    val centavosDesligados: Boolean = false,
    val centavos3Casas: Boolean = false,
    val centavosReduzidos: Boolean = true,
    val subtituloAtivo: Boolean = false,
    val duracaoIndex: Int = 0,
    val enabled: Boolean = true,
)

/**
 * Monta o quadro de uma oferta de preço — reimplementação limpa da ideia de
 * `Monta_Oferta` (Ofertas.pas): reais grandes + vírgula + centavos
 * (reduzidos/sobrescritos), barra de sublinhado, e os campos de texto,
 * tudo centralizado e ajustado ao tamanho do painel (meia/cheia).
 */
object OfertaLayout {

    private const val F_MICRO = 0   // 7x4
    private const val F_PEQ = 1     // 17x8
    private const val F_MED = 2     // 28x16
    private const val F_GRANDE = 3  // 42x24

    fun build(spec: OfertaSpec, fonts: FontProvider, halfScreen: Boolean, portrait: Boolean = false): PanelFrame {
        val panelW = if (portrait) 92 else if (halfScreen) 94 else 186
        val recs = mutableListOf<PanelRecord>()

        fun width(text: String, font: Int) = fonts.font(font).measure(AccentMap.normalize(text))
        fun centered(text: String, font: Int, row: Int, slot: Int) {
            if (text.isBlank()) return
            val x = ((panelW - width(text, font)) / 2).coerceAtLeast(1)
            recs.add(PanelRecord.Text(slot, row, x, font, text))
        }

        // --- topo: cabeçalho OU subtítulo (excludentes, como no app Windows:
        //     subtítulo ligado => Título + Subtítulo, sem cabeçalho) ---
        val usaSub = spec.subtituloAtivo
        val topo = if (usaSub) spec.subtitulo else spec.cabecalho
        centered(topo, F_PEQ, 2, if (usaSub) 3 else 1)
        centered(spec.titulo, F_MICRO, 20, 2)

        // --- preço ---
        val digits = spec.valor.filter { it.isDigit() }
        val centsLen = if (spec.centavos3Casas) 3 else 2
        val reais: String
        val cents: String?
        when {
            spec.centavosDesligados -> { reais = digits.ifEmpty { "0" }; cents = null }
            digits.isEmpty() -> { reais = "0"; cents = null }
            digits.length <= centsLen -> { reais = "0"; cents = digits.padStart(centsLen, '0') }
            else -> { reais = digits.dropLast(centsLen); cents = digits.takeLast(centsLen) }
        }

        val reaisFont = when {
            reais.length <= 2 -> F_GRANDE
            reais.length == 3 -> F_MED
            else -> F_PEQ
        }
        val reaisRow = 26
        val hReais = fonts.font(reaisFont).height
        val centsFont = if (spec.centavosReduzidos) F_PEQ else F_MED
        // vírgula + centavos como um bloco único ",90" (colado, sem folga)
        val centsText = cents?.let { ",$it" }

        val wReais = width(reais, reaisFont)
        val wCents = if (centsText != null) width(centsText, centsFont) else 0
        val totalW = wReais + wCents
        var x = ((panelW - totalW) / 2).coerceAtLeast(1)
        val priceStart = x

        recs.add(PanelRecord.Text(4, reaisRow, x, reaisFont, reais))
        x += wReais
        if (centsText != null) {
            val hCents = fonts.font(centsFont).height
            val centsRow =
                if (spec.centavosReduzidos) reaisRow + 2                    // sobrescrito (topo)
                else (reaisRow + hReais - hCents).coerceAtLeast(reaisRow)   // base
            recs.add(PanelRecord.Text(4, centsRow, x, centsFont, centsText))
            x += wCents
        }
        val priceEnd = (x - 1).coerceAtLeast(priceStart)

        // barra de sublinhado do preço
        val barRow = (reaisRow + hReais + 1).coerceAtMost(88)
        recs.add(PanelRecord.Graphic(barRow, priceStart, barRow, priceEnd))

        // medida + auxiliar (logo abaixo da barra) e rodapé (base)
        val infoRow = (barRow + 3).coerceAtMost(85)
        if (spec.medida.isNotBlank()) {
            recs.add(PanelRecord.Text(5, infoRow, priceStart, F_MICRO, spec.medida))
            if (spec.auxiliar.isNotBlank()) {
                val axX = priceStart + width(spec.medida, F_MICRO) + 4
                recs.add(PanelRecord.Text(7, infoRow, axX, F_MICRO, spec.auxiliar))
            }
        }
        centered(spec.rodape, F_MICRO, 84, 6)

        return PanelFrame(
            type = FrameType.OFERTA,
            halfScreen = halfScreen,
            durationIndex = spec.duracaoIndex,
            f3 = spec.subtituloAtivo,
            f4 = spec.centavosDesligados,
            f5 = spec.centavos3Casas,
            f6 = spec.centavosReduzidos,
            enabled = spec.enabled,
            records = recs,
        )
    }
}
