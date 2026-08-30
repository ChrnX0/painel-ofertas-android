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

    /** Alinhamento horizontal do bloco de texto. */
    enum class Align { LEFT, CENTER, RIGHT }

    /** Resultado da diagramação automática de um texto corrido. */
    data class Fit(
        val records: List<PanelRecord.Text>,
        val fontCode: Int,
        val lines: List<String>,
        val fits: Boolean,
    )

    /**
     * **Ajusta o texto ao espaço da tela.** Recebe o texto corrido e faz o trabalho
     * de um diagramador:
     *
     * 1. quebra em linhas por palavra (respeitando quebras que o usuário digitou);
     * 2. testa da maior fonte para a menor e escolhe **a maior em que tudo cabe** —
     *    largura *e* altura ao mesmo tempo;
     * 3. centraliza o bloco vertical e cada linha na horizontal.
     *
     * Se nem na menor fonte couber, devolve a melhor tentativa com `fits = false`
     * (a UI avisa que vai aparecer cortado).
     */
    fun fitParagraph(
        texto: String,
        halfScreen: Boolean,
        portrait: Boolean,
        fonts: FontProvider,
        maxFont: Int = 4,
        align: Align = Align.CENTER,
    ): Fit {
        val (cols, rows) = panelDims(halfScreen, portrait)
        if (texto.isBlank()) return Fit(emptyList(), 0, emptyList(), true)

        for (code in maxFont.coerceIn(0, 4) downTo 0) {
            val linhas = wrap(texto, code, cols, fonts)
            if (couberam(linhas, code, cols, rows, fonts)) {
                return Fit(layoutLines(linhas, code, cols, rows, fonts, align), code, linhas, true)
            }
        }
        val linhas = wrap(texto, 0, cols, fonts)
        return Fit(layoutLines(linhas, 0, cols, rows, fonts, align), 0, linhas, false)
    }

    /** O conjunto de linhas cabe na caixa (largura de cada uma e altura total)? */
    private fun couberam(linhas: List<String>, code: Int, cols: Int, rows: Int, fonts: FontProvider): Boolean {
        val f = fonts.font(code)
        if (linhas.any { f.measure(AccentMap.normalize(it)) > cols }) return false
        return alturaTotal(linhas.size, code, fonts) <= rows
    }

    private fun alturaTotal(n: Int, code: Int, fonts: FontProvider): Int {
        if (n <= 0) return 0
        val h = fonts.font(code).height
        return n * h + (n - 1) * entrelinha(h)
    }

    /** Espaço entre linhas, proporcional à altura da fonte (mínimo 1 pixel). */
    private fun entrelinha(alturaFonte: Int): Int = (alturaFonte / 5).coerceAtLeast(1)

    /**
     * Quebra o texto em linhas que caibam em [cols] com a fonte [code] — por
     * palavra; se uma palavra sozinha não couber, quebra dentro dela. Quebras de
     * linha digitadas pelo usuário são respeitadas.
     */
    fun wrap(texto: String, code: Int, cols: Int, fonts: FontProvider): List<String> {
        val f = fonts.font(code)
        fun largura(s: String) = f.measure(AccentMap.normalize(s))
        val saida = mutableListOf<String>()

        for (paragrafo in texto.split('\n')) {
            val palavras = paragrafo.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (palavras.isEmpty()) continue
            var linha = StringBuilder()

            fun fecharLinha() {
                if (linha.isNotEmpty()) { saida.add(linha.toString()); linha = StringBuilder() }
            }

            for (palavra in palavras) {
                val candidata = if (linha.isEmpty()) palavra else "$linha $palavra"
                when {
                    largura(candidata) <= cols -> { linha = StringBuilder(candidata) }
                    // não coube: fecha a linha atual e recomeça com esta palavra
                    largura(palavra) <= cols -> { fecharLinha(); linha = StringBuilder(palavra) }
                    // a palavra sozinha é maior que o painel: quebra dentro dela
                    else -> {
                        fecharLinha()
                        var pedaco = StringBuilder()
                        for (ch in palavra) {
                            if (largura("$pedaco$ch") <= cols) pedaco.append(ch)
                            else { saida.add(pedaco.toString()); pedaco = StringBuilder().append(ch) }
                        }
                        linha = pedaco
                    }
                }
            }
            fecharLinha()
        }
        return saida
    }

    /** Posiciona as linhas já quebradas: bloco centralizado na vertical. */
    private fun layoutLines(
        linhas: List<String>,
        code: Int,
        cols: Int,
        rows: Int,
        fonts: FontProvider,
        align: Align,
    ): List<PanelRecord.Text> {
        if (linhas.isEmpty()) return emptyList()
        val f = fonts.font(code)
        val h = f.height
        val gap = entrelinha(h)
        var y = ((rows - alturaTotal(linhas.size, code, fonts)) / 2).coerceAtLeast(0)

        return linhas.map { linha ->
            val larg = f.measure(AccentMap.normalize(linha))
            val x = when (align) {
                Align.LEFT -> 0
                Align.RIGHT -> (cols - larg).coerceAtLeast(0)
                Align.CENTER -> ((cols - larg) / 2).coerceAtLeast(0)
            }
            val rec = PanelRecord.Text(SLOT_LINHA_MENSAGEM, y, x, code, linha)
            y += h + gap
            rec
        }
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
