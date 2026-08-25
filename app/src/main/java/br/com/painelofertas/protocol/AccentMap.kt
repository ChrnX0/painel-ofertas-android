package br.com.painelofertas.protocol

/**
 * Normalização de texto para o painel — porte de `imprimir_tela` (Ofertas.pas:1258-1278).
 *
 * As fontes `.flb` só têm glifos para ASCII 32..108. Os textos são forçados a
 * MAIÚSCULAS, o que deixa livres os slots minúsculos `a`..`l` (97..108), usados
 * para guardar os 12 caracteres acentuados do português. Aqui remapeamos os
 * acentos para esses placeholders e trocamos qualquer caractere fora da faixa
 * (aspas tipográficas, emoji, etc.) por espaço.
 *
 * Isso também é uma **proteção de segurança do formato**: garante que nenhum
 * byte de texto caia em 0xFF (separador de bloco) ou 13 (CR terminador),
 * o que corromperia o parsing do álbum no painel.
 */
object AccentMap {

    private val toSlot: Map<Char, Char> = mapOf(
        'Á' to 'a', 'Ã' to 'b', 'É' to 'c', 'Ê' to 'd', 'Í' to 'e', 'Ó' to 'f',
        'Õ' to 'g', 'Ú' to 'h', 'Ç' to 'i', 'ª' to 'j', 'º' to 'k', '°' to 'l',
    )

    private val fromSlot: Map<Char, Char> = toSlot.entries.associate { (k, v) -> v to k }

    /**
     * Prepara o texto para renderização/gravação: MAIÚSCULAS, acentos -> `a`..`l`,
     * e caracteres fora de 32..108 viram espaço.
     */
    fun normalize(text: String): String = buildString {
        for (raw in text.uppercase()) {
            val mapped = toSlot[raw] ?: raw
            append(if (mapped.code in 32..108) mapped else ' ')
        }
    }

    /** Converte placeholders `a`..`l` de volta para os acentuados (exibição amigável). */
    fun denormalize(text: String): String = buildString {
        for (c in text) append(fromSlot[c] ?: c)
    }
}
