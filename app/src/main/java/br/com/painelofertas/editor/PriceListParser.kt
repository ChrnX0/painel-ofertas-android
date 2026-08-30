package br.com.painelofertas.editor

/**
 * Lê uma lista de preços exportada de planilha (CSV/TXT) e devolve uma linha de
 * oferta por produto, pronta para a criação em lote.
 *
 * Aceita os formatos que saem do Excel/Google Sheets e de sistemas de PDV:
 * ```
 * PICANHA;9,90;O KILO      → PICANHA 9,90 O KILO
 * ALCATRA<TAB>7,90         → ALCATRA 7,90
 * FRALDINHA,6.50,O KILO    → FRALDINHA 6,50 O KILO
 * PICANHA 9,90 O KILO      → (linha já pronta, mantida)
 * ```
 *
 * Cuidado deliberado com a vírgula: em `9,90` ela é decimal, não separador de
 * coluna. Por isso a vírgula só é tratada como separador quando o ponto e vírgula
 * e a tabulação não aparecem **e** o resultado ainda contém um preço válido.
 */
object PriceListParser {

    /** Preço no formato brasileiro ou internacional: 9,90 · 12.50 · 1.234,56 */
    private val PRECO = Regex("""\d{1,3}(?:\.\d{3})*[,.]\d{2}""")

    /** Cabeçalhos comuns de planilha, ignorados se aparecerem na 1ª linha. */
    private val CABECALHO = Regex("""^\s*(produto|descri|item|nome)\b""", RegexOption.IGNORE_CASE)

    /** Converte o conteúdo do arquivo em linhas de oferta (uma por produto). */
    fun parse(conteudo: String): List<String> {
        val linhas = conteudo.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val saida = mutableListOf<String>()

        for ((i, bruta) in linhas.withIndex()) {
            val linha = bruta.trim().trim('"')
            if (linha.isBlank()) continue
            if (i == 0 && CABECALHO.containsMatchIn(linha) && !PRECO.containsMatchIn(linha)) continue

            val campos = separarCampos(linha)
            val texto = campos.joinToString(" ") { it.trim().trim('"') }
                .replace(Regex("\\s+"), " ")
                .trim()
            if (texto.isNotBlank()) saida.add(normalizarPreco(texto))
        }
        return saida
    }

    /** Quebra a linha em campos, escolhendo o separador com segurança. */
    private fun separarCampos(linha: String): List<String> = when {
        linha.contains(';') -> linha.split(';')
        linha.contains('\t') -> linha.split('\t')
        // Vírgula só separa colunas se NÃO for a vírgula decimal de um preço.
        linha.contains(',') && !Regex("""\d,\d""").containsMatchIn(linha) -> linha.split(',')
        else -> listOf(linha)
    }

    /** Padroniza o preço para o formato brasileiro (12.50 → 12,50). */
    private fun normalizarPreco(texto: String): String =
        PRECO.replace(texto) { m ->
            val v = m.value
            // "1.234,56" já está em pt-BR; "12.50" tem ponto decimal → vira vírgula.
            if (v.count { it == '.' } == 1 && !v.contains(',')) v.replace('.', ',') else v
        }
}
