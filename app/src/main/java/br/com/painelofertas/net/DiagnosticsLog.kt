package br.com.painelofertas.net

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Console de diagnóstico TX/RX — porte do "Memo2" do app Windows, que não tinha
 * sido trazido. Registra o que o app **envia** ao painel e o que **recebe** de
 * volta, com hora, para depurar em campo ("cliquei e nada aconteceu").
 *
 * Anel de [capacity] linhas (as mais antigas caem). O tráfego de varredura
 * (`SERVIDOR=`, centenas por busca) é filtrado por quem chama, para não poluir.
 */
class DiagnosticsLog(private val capacity: Int = 200) {

    data class Line(val time: String, val dir: String, val peer: String, val text: String)

    private val _lines = MutableStateFlow<List<Line>>(emptyList())
    val lines: StateFlow<List<Line>> = _lines

    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private fun add(dir: String, peer: String, text: String) {
        val line = Line(fmt.format(Date()), dir, peer, text)
        _lines.value = (_lines.value + line).takeLast(capacity)
    }

    fun tx(peer: String, text: String) = add("TX", peer, text)
    fun rx(peer: String, text: String) = add("RX", peer, text)
    fun clear() { _lines.value = emptyList() }

    /** Descrição legível de um pacote: o texto ASCII, ou "PREFIXO…(NB)" se binário. */
    fun describe(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            val c = b.toInt() and 0xFF
            if (c in 32..126) sb.append(c.toChar()) else break
        }
        val ascii = sb.toString()
        return if (ascii.length >= bytes.size - 1) ascii.trimEnd()
        else "$ascii…(${bytes.size}B)"
    }
}
