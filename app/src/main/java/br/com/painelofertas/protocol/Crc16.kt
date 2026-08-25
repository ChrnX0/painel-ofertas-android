package br.com.painelofertas.protocol

/**
 * CRC-16/XMODEM (valor inicial 0, polinômio 0x1021, sem reflexão nem XOR final),
 * calculado sobre `bytes[0 .. size-3]` — ou seja, **excluindo os 2 últimos bytes**,
 * exatamente como `calculaCRC` (Ofertas.pas:5046-5073).
 *
 * O painel usa esse CRC para saber se já tem o conteúdo (evita reenvio).
 * Validado contra o vetor canônico "123456789" -> 0x31C3.
 */
object Crc16 {

    fun xmodem(bytes: IntArray): Int {
        var crc = 0
        val last = bytes.size - 3 // percorre 0..size-3 inclusive
        var i = 0
        while (i <= last) {
            crc = (crc xor (bytes[i] shl 8)) and 0xFFFF
            repeat(8) {
                crc = if (crc and 0x8000 != 0) {
                    (0x1021 xor (crc shl 1)) and 0xFFFF
                } else {
                    (crc shl 1) and 0xFFFF
                }
            }
            i++
        }
        return crc
    }
}
