package br.com.painelofertas.nfc

import android.app.Activity
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Parcelable

/**
 * Leitura de etiquetas NFC de painel. Fica **pronto** para os modelos com a
 * tecnologia: quando o celular encosta no painel, o app identifica qual é.
 *
 * Só depende do NFC do próprio celular (nada muda no protocolo do painel), então
 * já funciona hoje com uma etiqueta colada, e funcionará direto quando o painel
 * trouxer NFC de fábrica.
 */
object NfcReader {

    /** Estado do NFC neste aparelho — a UI explica o que fazer em cada caso. */
    enum class Estado { INDISPONIVEL, DESLIGADO, PRONTO }

    fun estado(activity: Activity): Estado {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return Estado.INDISPONIVEL
        return if (adapter.isEnabled) Estado.PRONTO else Estado.DESLIGADO
    }

    /**
     * Extrai a etiqueta de painel de um Intent de NFC (o Android entrega quando o
     * celular encosta numa tag). Devolve null se não for etiqueta LedBlock.
     */
    fun fromIntent(intent: Intent?): PanelTag? {
        if (intent == null) return null
        if (intent.action !in setOf(
                NfcAdapter.ACTION_NDEF_DISCOVERED,
                NfcAdapter.ACTION_TAG_DISCOVERED,
                NfcAdapter.ACTION_TECH_DISCOVERED,
            )
        ) return null

        val mensagens = mensagensNdef(intent) ?: return null
        for (msg in mensagens) {
            for (rec in msg.records) {
                val texto = textoDoRegistro(rec) ?: continue
                PanelTag.parse(texto)?.let { return it }
            }
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun mensagensNdef(intent: Intent): List<NdefMessage>? {
        val bruto: Array<out Parcelable>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, Parcelable::class.java)
            } else {
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            }
        return bruto?.filterIsInstance<NdefMessage>()?.takeIf { it.isNotEmpty() }
    }

    /** Texto de um registro NDEF: aceita registro de Texto e de URI. */
    private fun textoDoRegistro(rec: NdefRecord): String? = runCatching {
        when {
            rec.tnf == NdefRecord.TNF_WELL_KNOWN && rec.type.contentEquals(NdefRecord.RTD_TEXT) -> {
                // payload: [status][idioma...][texto UTF-8]
                val payload = rec.payload
                if (payload.isEmpty()) return@runCatching null
                val tamanhoIdioma = payload[0].toInt() and 0x3F
                String(payload, 1 + tamanhoIdioma, payload.size - 1 - tamanhoIdioma, Charsets.UTF_8)
            }
            rec.tnf == NdefRecord.TNF_WELL_KNOWN && rec.type.contentEquals(NdefRecord.RTD_URI) ->
                rec.toUri()?.toString()
            else -> String(rec.payload, Charsets.UTF_8)
        }
    }.getOrNull()
}
