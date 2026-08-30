package br.com.painelofertas.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Leitura de listas de preço exportadas de planilha. O ponto delicado é a
 * **vírgula**: em `9,90` ela é decimal, não separador de coluna.
 */
class PriceListParserTest {

    @Test
    fun `ponto e virgula separa colunas`() {
        val r = PriceListParser.parse("PICANHA;9,90;O KILO")
        assertEquals(listOf("PICANHA 9,90 O KILO"), r)
    }

    @Test
    fun `tabulacao separa colunas`() {
        val r = PriceListParser.parse("ALCATRA\t7,90\tO KILO")
        assertEquals(listOf("ALCATRA 7,90 O KILO"), r)
    }

    @Test
    fun `virgula decimal nao e tratada como separador`() {
        // Só há vírgula, e ela é decimal: a linha deve ficar inteira.
        val r = PriceListParser.parse("FRALDINHA 6,50 O KILO")
        assertEquals(listOf("FRALDINHA 6,50 O KILO"), r)
    }

    @Test
    fun `virgula separa colunas quando o preco usa ponto`() {
        val r = PriceListParser.parse("FRALDINHA,6.50,O KILO")
        // e o preço é normalizado para o formato brasileiro
        assertEquals(listOf("FRALDINHA 6,50 O KILO"), r)
    }

    @Test
    fun `preco com ponto vira virgula`() {
        assertEquals(listOf("QUEIJO 12,50 O KILO"), PriceListParser.parse("QUEIJO;12.50;O KILO"))
    }

    @Test
    fun `milhar em pt-BR e preservado`() {
        assertEquals(listOf("TV 1.234,56 A VISTA"), PriceListParser.parse("TV;1.234,56;A VISTA"))
    }

    @Test
    fun `cabecalho de planilha e ignorado`() {
        val csv = "Produto;Preco;Medida\nPICANHA;9,90;O KILO\nALCATRA;7,90;O KILO"
        val r = PriceListParser.parse(csv)
        assertEquals(listOf("PICANHA 9,90 O KILO", "ALCATRA 7,90 O KILO"), r)
    }

    @Test
    fun `linhas vazias e aspas sao limpas`() {
        val csv = "\"PICANHA\";\"9,90\";\"O KILO\"\n\n   \n"
        assertEquals(listOf("PICANHA 9,90 O KILO"), PriceListParser.parse(csv))
    }

    @Test
    fun `arquivo com CRLF do Windows`() {
        val r = PriceListParser.parse("PICANHA;9,90\r\nALCATRA;7,90\r\n")
        assertEquals(listOf("PICANHA 9,90", "ALCATRA 7,90"), r)
    }

    @Test
    fun `produto sem preco ainda vira linha`() {
        val r = PriceListParser.parse("BEM VINDOS A NOSSA LOJA")
        assertTrue(r.isNotEmpty())
    }
}
