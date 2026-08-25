package br.com.painelofertas.protocol

/** Resultado de compilar um álbum para o fluxo de bytes enviado ao painel. */
class CompileResult(
    val bytes: IntArray,   // cada elemento é um byte 0..255
    val crc: Int,
    val consumo: Int,      // = bytes.size (quantos bytes o conteúdo ocupa na memória do painel)
    val brilho: Int,       // 0..100
)

/**
 * Codec binário do conteúdo do painel — porte **fiel** de `processaArquivo` e
 * `construirArquivo` (Ofertas.pas). Opera sobre "linhas" no mesmo formato-texto
 * do app Windows: um álbum `.alb` = 4 linhas de cabeçalho + blocos `:`/`;`.
 *
 * Gramática dos blocos:
 *  - Cabeçalho de quadro: `:TYPE;ADSIZE;DUR;F3;F4;F5;F6;ENABLE;`
 *  - Linha de texto:      `;LEN;SLOT;ROW;COL;FONT;TEXTO`
 *  - Linha de gráfico:    `;5;0;ROW1;COL1;ROW2;COL2;` (SLOT=0)
 *
 * No binário: separador de bloco = 0xFF; fim do fluxo = 0xFF 0xFF.
 * Validado byte-a-byte contra `mensagem.dll` (111 bytes) e `nelPai.dll` (49 bytes).
 */
object BinaryCodec {

    /**
     * `processaArquivo` (Ofertas.pas:5222-5333): linhas de um `.alb` -> bytes.
     * [albLines] deve incluir as 4 linhas de cabeçalho (nome/CRC/consumo/brilho).
     */
    fun compile(albLines: List<String>): CompileResult {
        val brilho = albLines.getOrNull(3)?.trim()?.toIntOrNull() ?: 100

        // 1ª passada: soma o tamanho total (o campo LEN de cada `;` = bytes daquele registro).
        var acumulador = 0
        for (raw in albLines) {
            if (raw.isEmpty()) continue
            when (raw[0]) {
                ':' -> acumulador += 3
                ';' -> acumulador += retornaParametro(raw, 1, ';', ';', 'z').toIntOrNull() ?: 0
            }
        }
        val bytes = IntArray(acumulador + 1) { 255 }

        // 2ª passada: escreve os bytes.
        var idx = 0
        for (raw in albLines) {
            if (raw.isEmpty()) continue
            when (raw[0]) {
                ':' -> {
                    if (idx > 0) { bytes[idx] = 255; idx++ } // separador de blocos
                    var acc = 0
                    if (retornaParametro(raw, 1, ':', ';', 'z') == "1") acc += 64
                    if (retornaParametro(raw, 1, ';', ';', 'z') == "1") acc += 32
                    if (retornaParametro(raw, 3, ';', ';', 'z') == "1") acc += 16
                    if (retornaParametro(raw, 4, ';', ';', 'z') == "1") acc += 8
                    if (retornaParametro(raw, 5, ';', ';', 'z') == "1") acc += 4
                    if (retornaParametro(raw, 6, ';', ';', 'z') == "1") acc += 2
                    if (retornaParametro(raw, 7, ';', ';', 'z') == "1") acc += 1
                    bytes[idx] = acc; idx++
                    bytes[idx] = DurationTable.idxToByte(retornaParametro(raw, 2, ';', ';', 'z')); idx++
                }
                ';' -> {
                    val slot = retornaParametro(raw, 2, ';', ';', 'z').toIntOrNull() ?: 0
                    bytes[idx] = slot
                    if (slot == 0) { // gráfico: [0][ROW1][COL1][ROW2][COL2]
                        idx++; bytes[idx] = retornaParametro(raw, 3, ';', ';', 'z').toIntOrNull() ?: 0
                        idx++; bytes[idx] = retornaParametro(raw, 4, ';', ';', 'z').toIntOrNull() ?: 0
                        idx++; bytes[idx] = retornaParametro(raw, 5, ';', ';', 'z').toIntOrNull() ?: 0
                        idx++; bytes[idx] = retornaParametro(raw, 6, ';', ';', 'z').toIntOrNull() ?: 0
                        idx++
                    } else {         // texto: [SLOT][ROW][COL][FONT][ascii...][13]
                        idx++; bytes[idx] = retornaParametro(raw, 3, ';', ';', 'z').toIntOrNull() ?: 0
                        idx++; bytes[idx] = retornaParametro(raw, 4, ';', ';', 'z').toIntOrNull() ?: 0
                        idx++; bytes[idx] = retornaParametro(raw, 5, ';', ';', 'z').toIntOrNull() ?: 0
                        idx++
                        // Normaliza para o fio: MAIÚSCULAS, acentos -> a..l, e nada fora de
                        // 32..108 (evita injetar 0xFF/CR e garante o glifo certo no painel).
                        val texto = AccentMap.normalize(campoTexto(raw))
                        for (c in texto) { bytes[idx] = c.code and 0xFF; idx++ }
                        bytes[idx] = 13; idx++
                    }
                }
            }
        }

        val crc = Crc16.xmodem(bytes)
        return CompileResult(bytes, crc, bytes.size, brilho)
    }

    /**
     * `construirArquivo` (Ofertas.pas:5075-5178): bytes -> linhas de bloco `:`/`;`.
     * Não inclui as 4 linhas de cabeçalho do `.alb` (só os blocos).
     */
    fun decompile(rxbytes: IntArray): List<String> {
        val linhas = mutableListOf<String>()
        var ptr = 0
        var separador = 1
        val n = rxbytes.size

        while (ptr < n) {
            if (rxbytes[ptr] == 255) {
                separador = 1
                if (ptr + 1 < n && rxbytes[ptr + 1] == 255) break // fim do fluxo
                ptr++
                continue
            }

            val apoio: String
            if (separador == 1) { // cabeçalho de quadro
                separador = 0
                val b = rxbytes[ptr]
                val sb = StringBuilder(":")
                sb.append(if (b and 64 != 0) "1;" else "0;")
                sb.append(if (b and 32 != 0) "1;" else "0;")
                sb.append(DurationTable.byteToIdx(rxbytes[ptr + 1])).append(';')
                sb.append(if (b and 16 != 0) "1;" else "0;")
                sb.append(if (b and 8 != 0) "1;" else "0;")
                sb.append(if (b and 4 != 0) "1;" else "0;")
                sb.append(if (b and 2 != 0) "1;" else "0;")
                sb.append(if (b and 1 != 0) "1;" else "0;")
                apoio = sb.toString()
                ptr++ // consome flags; o byte de duração é consumido pelo ptr++ do fim do laço
            } else if (rxbytes[ptr] == 0) { // gráfico
                val sb = StringBuilder(";5;0;")
                ptr++; sb.append(rxbytes[ptr]).append(';')
                ptr++; sb.append(rxbytes[ptr]).append(';')
                ptr++; sb.append(rxbytes[ptr]).append(';')
                ptr++; sb.append(rxbytes[ptr]).append(';')
                apoio = sb.toString()
            } else { // texto
                var comprimento = 4
                var t = 4
                while (ptr + t < n) {
                    comprimento++
                    if (rxbytes[ptr + t] < 32 || rxbytes[ptr + t] > 126) break
                    t++
                }
                val sb = StringBuilder(";")
                sb.append(comprimento).append(';')
                sb.append(rxbytes[ptr]).append(';')
                ptr++; sb.append(rxbytes[ptr]).append(';')
                ptr++; sb.append(rxbytes[ptr]).append(';')
                ptr++; sb.append(rxbytes[ptr]).append(';')
                while (true) {
                    ptr++
                    if (ptr >= n || rxbytes[ptr] < 32 || rxbytes[ptr] > 126) break
                    sb.append(rxbytes[ptr].toChar())
                }
                apoio = sb.toString()
            }

            linhas.add(apoio)
            ptr++
        }
        return linhas
    }

    /**
     * Texto de um registro `;LEN;SLOT;ROW;COL;FONT;TEXTO`: tudo após o 6º ';'.
     * Corrige de propósito o "bug do 'z'" do original (que truncava o texto na
     * releitura ao encontrar um 'z' minúsculo).
     */
    fun campoTexto(linha: String): String {
        var count = 0
        var i = 0
        while (i < linha.length) {
            if (linha[i] == ';') {
                count++
                if (count == 6) return linha.substring(i + 1)
            }
            i++
        }
        return ""
    }
}
