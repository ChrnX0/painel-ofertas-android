package br.com.painelofertas.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes do motor de diagramação automática. Usa fontes sintéticas de largura
 * fixa (não depende dos arquivos `.flb`), então as contas são previsíveis:
 * a fonte de código `c` tem altura `c+1` e cada caractere ocupa `c+1` colunas.
 */
private val fakeFonts = FontProvider { code ->
    val lado = code + 1
    // Glifo quadrado de lado `lado`, sem espaçamento extra: measure = n * lado.
    val linhas = buildList {
        add("cabecalho")
        // um glifo por caractere possível; todos iguais
        repeat(240) { add(";$lado:$lado:0:0:${lado}:;" + List(lado) { "255" }.joinToString(":") + ":") }
    }
    FlbFont(linhas)
}

class AutoLayoutTest {

    @Test
    fun `quebra por palavra respeitando a largura`() {
        // fonte 0: 1 coluna por caractere. Largura 10 -> "ABC DEF GH" quebra em duas linhas.
        val linhas = AutoLayout.wrap("ABC DEF GHIJ", code = 0, cols = 8, fonts = fakeFonts)
        assertEquals(listOf("ABC DEF", "GHIJ"), linhas)
    }

    @Test
    fun `palavra maior que o painel e quebrada dentro dela`() {
        val linhas = AutoLayout.wrap("ABCDEFGHIJ", code = 0, cols = 4, fonts = fakeFonts)
        assertEquals(listOf("ABCD", "EFGH", "IJ"), linhas)
    }

    @Test
    fun `quebras digitadas pelo usuario sao respeitadas`() {
        val linhas = AutoLayout.wrap("UM\nDOIS", code = 0, cols = 50, fonts = fakeFonts)
        assertEquals(listOf("UM", "DOIS"), linhas)
    }

    @Test
    fun `escolhe a maior fonte em que tudo cabe`() {
        // Painel de meia tela: 96 colunas x 92 linhas.
        // "OI" na fonte 4 mede 2*5 = 10 <= 96 e altura 5 <= 92 -> deve usar a maior (4).
        val fit = AutoLayout.fitParagraph("OI", halfScreen = true, portrait = false, fonts = fakeFonts)
        assertTrue(fit.fits)
        assertEquals(4, fit.fontCode)
        assertEquals(listOf("OI"), fit.lines)
    }

    @Test
    fun `reduz a fonte quando o texto e longo`() {
        // Texto que na fonte 4 exigiria ~20 linhas (20*5 + 19 = 119 > 92 de altura):
        // o motor precisa descer de tamanho até o bloco caber na ALTURA também.
        val texto = List(40) { "PROMOCAO" }.joinToString(" ")
        val fit = AutoLayout.fitParagraph(texto, halfScreen = true, portrait = false, fonts = fakeFonts)
        assertTrue("deveria caber reduzindo a fonte", fit.fits)
        assertTrue("esperava fonte menor que a maxima (usou ${fit.fontCode})", fit.fontCode < 4)
    }

    @Test
    fun `bloco fica centralizado verticalmente e dentro do painel`() {
        val fit = AutoLayout.fitParagraph("UM DOIS TRES", halfScreen = true, portrait = false, fonts = fakeFonts)
        val alturaFonte = fit.fontCode + 1
        val primeira = fit.records.first().row
        val ultima = fit.records.last().row + alturaFonte
        assertTrue("nao pode comecar acima do topo", primeira >= 0)
        assertTrue("nao pode passar da altura do painel", ultima <= 92)
        // sobra em cima ~ sobra embaixo (centralizado)
        assertTrue("bloco deveria estar centralizado", kotlin.math.abs(primeira - (92 - ultima)) <= 2)
    }

    @Test
    fun `texto que nao cabe e sinalizado`() {
        // Muitas linhas: nem na menor fonte (altura 1 + 1 de entrelinha) cabe em 92.
        val texto = (1..60).joinToString("\n") { "LINHA $it" }
        val fit = AutoLayout.fitParagraph(texto, halfScreen = true, portrait = false, fonts = fakeFonts)
        assertFalse("deveria sinalizar que nao cabe", fit.fits)
    }

    @Test
    fun `alinhamento a esquerda comeca na coluna zero`() {
        val fit = AutoLayout.fitParagraph(
            "OI", halfScreen = true, portrait = false, fonts = fakeFonts, align = AutoLayout.Align.LEFT,
        )
        assertEquals(0, fit.records.first().col)
    }

    // ===== Composição inteligente (destaque do preço) =====

    @Test
    fun `separa produto preco e medida de uma linha so`() {
        val partes = AutoLayout.smartSplit("PICANHA 9,90 O KILO")
        assertEquals(listOf("PICANHA", "9,90", "O KILO"), partes.map { it.text })
        assertEquals(listOf(false, true, false), partes.map { it.hero })
    }

    @Test
    fun `reconhece preco com ponto e com milhar`() {
        assertEquals("12.50", AutoLayout.smartSplit("QUEIJO 12.50 KG").first { it.hero }.text)
        assertEquals("1.234,56", AutoLayout.smartSplit("TV 1.234,56 A VISTA").first { it.hero }.text)
    }

    @Test
    fun `nao confunde medida com preco`() {
        // "100 G" e "CX 12" nao tem separador decimal: nada vira destaque.
        assertTrue(AutoLayout.smartSplit("PRESUNTO 100 G").none { it.hero })
        assertTrue(AutoLayout.smartSplit("CERVEJA CX 12").none { it.hero })
    }

    @Test
    fun `respeita as quebras do usuario e so marca o preco`() {
        val partes = AutoLayout.smartSplit("OFERTA\n9,90\nO KILO")
        assertEquals(listOf("OFERTA", "9,90", "O KILO"), partes.map { it.text })
        assertEquals(1, partes.count { it.hero })
    }

    @Test
    fun `preco fica maior que o resto do texto`() {
        val fit = AutoLayout.smartFit("PICANHA 9,90 O KILO", halfScreen = true, portrait = false, fonts = fakeFonts)
        assertTrue(fit.fits)
        val preco = fit.records.first { it.text == "9,90" }
        val outros = fit.records.filter { it.text != "9,90" }
        assertTrue("o preco deveria usar fonte maior", outros.all { it.font < preco.font })
    }

    @Test
    fun `sem preco a composicao inteligente cai no ajuste normal`() {
        val fit = AutoLayout.smartFit("BEM VINDOS", halfScreen = true, portrait = false, fonts = fakeFonts)
        assertTrue(fit.fits)
        assertTrue(fit.records.map { it.font }.distinct().size == 1)
    }

    @Test
    fun `texto vazio nao gera registros`() {
        val fit = AutoLayout.fitParagraph("   ", halfScreen = true, portrait = false, fonts = fakeFonts)
        assertTrue(fit.records.isEmpty())
        assertTrue(fit.fits)
    }
}
