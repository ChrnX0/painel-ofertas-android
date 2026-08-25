package br.com.painelofertas.protocol

/**
 * Extração de campo idêntica a `retorna_parametro_string` (Ofertas.pas:777-796).
 *
 * Varre a string decrementando [posicao] a cada ocorrência de [charInicio]; quando
 * chega a zero, passa a coletar caracteres até encontrar [charFim], ignorando
 * qualquer [charIgnore]. É a base de todo o parsing do formato de mensagem.
 */
fun retornaParametro(
    origem: String,
    posicao: Int,
    charInicio: Char,
    charFim: Char,
    charIgnore: Char,
): String {
    var pos = posicao
    val sb = StringBuilder()
    for (ch in origem) {
        if (pos == 0) {
            if (ch == charFim) break
            else if (ch != charIgnore) sb.append(ch)
        } else {
            if (ch == charInicio) pos--
        }
    }
    return sb.toString()
}

/**
 * Tabela de duração dos quadros: índice do ComboBox (0..16) <-> byte gravado
 * no fluxo binário (Ofertas.pas:5271-5288). Índice 0 = "Automático".
 */
object DurationTable {

    /** Segundos exibidos para cada índice 0..16 (0 = automático). */
    val secondsByIndex = intArrayOf(0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 25, 30, 40, 50, 60)

    private val idxToByteMap: Map<String, Int> =
        secondsByIndex.mapIndexed { i, seg -> i.toString() to seg }.toMap()

    private val byteToIdxMap: Map<Int, String> =
        secondsByIndex.mapIndexed { i, seg -> seg to i.toString() }.toMap()

    /** índice (ex.: "10") -> byte (ex.: 15). Desconhecido -> 0. */
    fun idxToByte(idx: String): Int = idxToByteMap[idx] ?: 0

    /** byte (ex.: 15) -> índice (ex.: "10"). Desconhecido -> "0". */
    fun byteToIdx(b: Int): String = byteToIdxMap[b] ?: "0"
}
