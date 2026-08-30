package br.com.painelofertas.editor

import br.com.painelofertas.render.AutoLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Criação em lote: cada linha da lista precisa virar uma tela com o texto certo,
 * o preço reconhecido e um nome curto tirado do produto.
 *
 * Testa a lógica pura (separação + nome), sem depender do ViewModel/Android.
 */
class BatchTest {

    /** Espelha `EditorViewModel.nomeDaOferta` — nome curto a partir da linha. */
    private fun nomeDaOferta(linha: String): String {
        val antes = AutoLayout.smartSplit(linha).firstOrNull()?.takeIf { !it.hero }?.text
        val base = antes ?: linha
        return base.split(Regex("\\s+")).take(2).joinToString(" ")
            .lowercase().replaceFirstChar { it.uppercase() }
    }

    @Test
    fun `nome da tela vem do produto antes do preco`() {
        assertEquals("Picanha", nomeDaOferta("PICANHA 9,90 O KILO"))
        assertEquals("File mignon", nomeDaOferta("FILE MIGNON 49,90 O KILO"))
    }

    @Test
    fun `nome usa no maximo duas palavras`() {
        assertEquals("Coxa e", nomeDaOferta("COXA E SOBRECOXA 8,90 O KILO"))
    }

    @Test
    fun `linha sem preco ainda gera nome`() {
        assertEquals("Bem vindos", nomeDaOferta("BEM VINDOS A NOSSA LOJA"))
    }

    @Test
    fun `cada linha da lista vira uma oferta com preco destacado`() {
        val lista = """
            PICANHA 9,90 O KILO
            ALCATRA 7,90 O KILO
            FRALDINHA 6,50 O KILO
        """.trimIndent()

        val linhas = lista.split('\n').map { it.trim() }.filter { it.isNotBlank() }
        assertEquals(3, linhas.size)

        linhas.forEach { linha ->
            val partes = AutoLayout.smartSplit(linha)
            assertEquals("deveria separar em produto/preco/medida", 3, partes.size)
            assertTrue("o preco deveria ser o destaque", partes[1].hero)
        }

        assertEquals(listOf("Picanha", "Alcatra", "Fraldinha"), linhas.map { nomeDaOferta(it) })
    }

    @Test
    fun `linhas em branco sao ignoradas`() {
        val lista = "PICANHA 9,90 O KILO\n\n   \nALCATRA 7,90 O KILO\n"
        val linhas = lista.split('\n').map { it.trim() }.filter { it.isNotBlank() }
        assertEquals(2, linhas.size)
    }
}
