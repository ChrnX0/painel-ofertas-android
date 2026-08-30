package br.com.painelofertas.render

import br.com.painelofertas.protocol.AccentMap
import br.com.painelofertas.protocol.PanelRecord

/**
 * Posicionamento automático de texto no painel — o "auto-justificar" do modo Livre.
 *
 * O lojista escreve o texto e o app decide **onde** ele fica: centralizado na
 * largura, distribuído na altura, e (opcionalmente) na maior fonte em que ainda
 * cabe. Também detecta quando o conteúdo **estoura** o painel, para avisar antes
 * de mandar algo que apareceria cortado no display.
 */
object AutoLayout {

    /** Dimensões do painel em (colunas, linhas), conforme meia/cheia e orientação. */
    fun panelDims(halfScreen: Boolean, portrait: Boolean): Pair<Int, Int> {
        val (w, h) = if (halfScreen) 96 to 92 else 188 to 92
        return if (portrait) h to w else w to h
    }

    /** A maior fonte (código 0..4) em que [text] cabe em [cols] — ou a menor, se nenhuma couber. */
    fun bestFont(text: String, cols: Int, fonts: FontProvider, maxFont: Int = 4): Int {
        val t = AccentMap.normalize(text)
        for (code in maxFont downTo 0) {
            if (fonts.font(code).measure(t) <= cols) return code
        }
        return 0
    }

    /** Coluna que centraliza [text] na largura [cols] (nunca negativa). */
    fun centerCol(text: String, fontCode: Int, cols: Int, fonts: FontProvider): Int {
        val w = fonts.font(fontCode).measure(AccentMap.normalize(text))
        return ((cols - w) / 2).coerceAtLeast(0)
    }

    /**
     * Posiciona as [linhas] sozinho: centraliza cada uma na horizontal e distribui
     * o bloco na vertical, com espaçamento uniforme. Se [autoFont], também escolhe
     * a maior fonte que couber na largura (cada linha independente).
     */
    fun layout(
        linhas: List<Pair<String, Int>>, // texto -> fonte pedida
        halfScreen: Boolean,
        portrait: Boolean,
        fonts: FontProvider,
        autoFont: Boolean = true,
    ): List<PanelRecord.Text> {
        val (cols, rows) = panelDims(halfScreen, portrait)
        val validas = linhas.filter { it.first.isNotBlank() }
        if (validas.isEmpty()) return emptyList()

        // fonte de cada linha: a maior que cabe (ou a pedida, se couber)
        val fontes = validas.map { (texto, pedida) ->
            if (autoFont) minOf(pedida, bestFont(texto, cols, fonts, maxFont = pedida.coerceIn(0, 4)))
            else pedida
        }

        val alturas = validas.indices.map { i -> fonts.font(fontes[i]).height }
        val alturaTotal = alturas.sum()
        // sobra dividida em (n+1) espaços iguais: acima, entre linhas e abaixo
        val folga = (rows - alturaTotal).coerceAtLeast(0)
        val gap = folga / (validas.size + 1)

        var y = gap
        return validas.mapIndexed { i, (texto, _) ->
            val fonte = fontes[i]
            val rec = PanelRecord.Text(
                slot = SLOT_LINHA_MENSAGEM,
                row = y,
                col = centerCol(texto, fonte, cols, fonts),
                font = fonte,
                text = texto,
            )
            y += alturas[i] + gap
            rec
        }
    }

    /** O que estourou o painel (em pixels), se algo estourou. */
    data class Overflow(val larguraExcedida: Int, val alturaExcedida: Int) {
        val houve: Boolean get() = larguraExcedida > 0 || alturaExcedida > 0
    }

    /**
     * Verifica se o conteúdo cabe no painel. Retorna quanto passou em cada eixo —
     * a UI usa para avisar "o texto vai aparecer cortado".
     */
    fun overflow(
        records: List<PanelRecord>,
        halfScreen: Boolean,
        portrait: Boolean,
        fonts: FontProvider,
    ): Overflow {
        val (cols, rows) = panelDims(halfScreen, portrait)
        val (maxCol, maxRow) = PanelRenderer.contentBounds(records, fonts)
        return Overflow(
            larguraExcedida = (maxCol - cols).coerceAtLeast(0),
            alturaExcedida = (maxRow - rows).coerceAtLeast(0),
        )
    }

    /** Slot semântico das linhas de uma Mensagem (conforme o protocolo). */
    private const val SLOT_LINHA_MENSAGEM = 9
}
