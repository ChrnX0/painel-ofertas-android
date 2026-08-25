package br.com.painelofertas.render

import br.com.painelofertas.protocol.retornaParametro
import kotlin.math.ceil

/**
 * Um glifo bitmap: altura, largura, espaçamento à direita e os bytes de dados
 * (organizados linha a linha; cada byte cobre 8 colunas, bit menos significativo
 * = coluna mais à esquerda).
 */
class Glyph(
    val height: Int,
    val width: Int,
    val spacing: Int,
    val bytesPerRow: Int,
    val data: IntArray,
) {
    /** Pixel (row, col) do glifo aceso? */
    fun isLit(row: Int, col: Int): Boolean {
        if (row < 0 || row >= height || col < 0 || col >= width) return false
        val i = row * bytesPerRow + (col / 8)
        if (i < 0 || i >= data.size) return false
        return (data[i] shr (col % 8)) and 1 != 0
    }

    /** Avanço horizontal após desenhar este caractere (largura + espaçamento). */
    val advance: Int get() = width + spacing
}

/**
 * Fonte bitmap `.flb` — porte fiel do parser de `imprimir_tela` (Ofertas.pas:1240-1417).
 *
 * O arquivo é uma lista de linhas; a linha 0 é um cabeçalho e o glifo do caractere
 * `c` fica no índice `c.code - 31` (ex.: espaço/ASCII 32 -> índice 1). Cada glifo:
 * `;H:W:?:S:BYTES:;d0:d1:...:` (campos separados por `:`, dados a partir do 5º campo).
 */
class FlbFont(private val lines: List<String>) {

    private val cache = HashMap<Char, Glyph>()

    /** Glifo de [c] (já espera um caractere de slot: use [AccentMap.normalize] antes). */
    fun glyph(c: Char): Glyph = cache.getOrPut(c) { parse(c) }

    private fun parse(c: Char): Glyph {
        val idx = c.code - 31
        val line = lines.getOrNull(idx) ?: return empty()
        val h = retornaParametro(line, 1, ';', ':', ']').toIntOrNull() ?: return empty()
        if (h <= 0) return empty()
        val w = retornaParametro(line, 1, ':', ':', ']').toIntOrNull() ?: 0
        val s = retornaParametro(line, 3, ':', ':', ']').toIntOrNull() ?: 0
        val nbytes = retornaParametro(line, 4, ':', ':', ']').toIntOrNull() ?: 0
        val bytesPerRow = ceil(nbytes.toDouble() / h).toInt().coerceAtLeast(1)
        val data = IntArray(nbytes)
        for (k in 0 until nbytes) {
            data[k] = retornaParametro(line, 5 + k, ':', ':', ';').toIntOrNull() ?: 0
        }
        return Glyph(h, w, s, bytesPerRow, data)
    }

    /** Largura em pixels que [text] (já normalizado) ocupa nesta fonte. */
    fun measure(text: String): Int = text.sumOf { glyph(it).advance }

    /** Altura (linhas) da fonte — igual para todos os glifos do arquivo. */
    val height: Int get() = glyph('0').height

    private fun empty() = Glyph(DEFAULT_HEIGHT, 0, 1, 1, IntArray(0))

    companion object {
        private const val DEFAULT_HEIGHT = 7

        /** Cria a fonte a partir do conteúdo bruto de um `.flb` (tolerante a CRLF/LF). */
        fun fromText(text: String): FlbFont =
            FlbFont(text.replace("\r\n", "\n").split('\n'))
    }
}
