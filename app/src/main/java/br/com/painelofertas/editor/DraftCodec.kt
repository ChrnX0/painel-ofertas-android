package br.com.painelofertas.editor

import br.com.painelofertas.render.AutoLayout
import br.com.painelofertas.render.OfertaSpec
import org.json.JSONArray
import org.json.JSONObject

/**
 * Serializa o **estado do editor** (não o `.alb`) para o rascunho automático.
 *
 * O formato `.alb` guarda só o resultado renderizado — ao recarregá-lo, uma Oferta
 * volta como quadro "cru", sem os campos (preço, cabeçalho…) que o lojista digitou.
 * Este codec preserva o rascunho de verdade, então o trabalho volta **editável**.
 */
object DraftCodec {

    fun encode(nome: String, portrait: Boolean, frames: List<FrameDraft>): String =
        JSONObject().apply {
            put("nome", nome)
            put("portrait", portrait)
            put("frames", JSONArray().also { arr -> frames.forEach { arr.put(encodeFrame(it)) } })
        }.toString()

    /** Devolve (nome, portrait, frames) ou null se o texto não for um rascunho válido. */
    fun decode(text: String): Triple<String, Boolean, List<FrameDraft>>? = runCatching {
        val o = JSONObject(text)
        val arr = o.getJSONArray("frames")
        val frames = (0 until arr.length()).mapNotNull { decodeFrame(arr.getJSONObject(it)) }
        if (frames.isEmpty()) null
        else Triple(o.optString("nome", "Álbum 1"), o.optBoolean("portrait", false), frames)
    }.getOrNull()

    private fun encodeFrame(d: FrameDraft): JSONObject = when (d) {
        is FrameDraft.Ofe -> JSONObject().apply {
            put("tipo", "oferta")
            put("nome", d.name)
            put("meia", d.halfScreen)
            put("spec", JSONObject().apply {
                val s = d.spec
                put("cabecalho", s.cabecalho); put("titulo", s.titulo); put("subtitulo", s.subtitulo)
                put("valor", s.valor); put("medida", s.medida); put("auxiliar", s.auxiliar); put("rodape", s.rodape)
                put("centsOff", s.centavosDesligados); put("cents3", s.centavos3Casas)
                put("centsRed", s.centavosReduzidos); put("subAtivo", s.subtituloAtivo)
                put("duracao", s.duracaoIndex); put("enabled", s.enabled)
            })
        }
        is FrameDraft.Msg -> JSONObject().apply {
            put("tipo", "mensagem")
            put("nome", d.name)
            put("meia", d.halfScreen)
            put("duracao", d.durationIndex)
            put("borda", d.border)
            put("enabled", d.enabled)
            put("autoFit", d.autoFit)
            put("freeText", d.freeText)
            put("align", d.align.name)
            put("maxFont", d.maxFont)
            put("smart", d.smart)
            put("linhas", JSONArray().also { arr ->
                d.lines.forEach { l ->
                    arr.put(JSONObject().apply {
                        put("texto", l.text); put("fonte", l.font); put("linha", l.row); put("coluna", l.col)
                    })
                }
            })
        }
        // Quadros "crus" (vindos de um álbum antigo) não têm campos para preservar.
        is FrameDraft.Raw -> JSONObject().put("tipo", "cru")
    }

    private fun decodeFrame(o: JSONObject): FrameDraft? = when (o.optString("tipo")) {
        "oferta" -> {
            val s = o.getJSONObject("spec")
            FrameDraft.Ofe(
                spec = OfertaSpec(
                    cabecalho = s.optString("cabecalho"), titulo = s.optString("titulo"),
                    subtitulo = s.optString("subtitulo"), valor = s.optString("valor"),
                    medida = s.optString("medida"), auxiliar = s.optString("auxiliar"),
                    rodape = s.optString("rodape"),
                    centavosDesligados = s.optBoolean("centsOff"), centavos3Casas = s.optBoolean("cents3"),
                    centavosReduzidos = s.optBoolean("centsRed", true), subtituloAtivo = s.optBoolean("subAtivo"),
                    duracaoIndex = s.optInt("duracao"), enabled = s.optBoolean("enabled", true),
                ),
                halfScreen = o.optBoolean("meia", true),
                name = o.optString("nome"),
            )
        }
        "mensagem" -> {
            val arr = o.optJSONArray("linhas") ?: JSONArray()
            val linhas = (0 until arr.length()).map { i ->
                val l = arr.getJSONObject(i)
                LineDraft(l.optString("texto"), l.optInt("fonte", 1), l.optInt("linha", 4), l.optInt("coluna", 4))
            }.ifEmpty { listOf(LineDraft()) }
            FrameDraft.Msg(
                lines = linhas,
                halfScreen = o.optBoolean("meia", true),
                durationIndex = o.optInt("duracao"),
                border = o.optInt("borda"),
                enabled = o.optBoolean("enabled", true),
                name = o.optString("nome"),
                autoFit = o.optBoolean("autoFit", true),
                freeText = o.optString("freeText"),
                align = runCatching { AutoLayout.Align.valueOf(o.optString("align", "CENTER")) }
                    .getOrDefault(AutoLayout.Align.CENTER),
                maxFont = o.optInt("maxFont", 4),
                smart = o.optBoolean("smart", true),
            )
        }
        else -> null // "cru" não é restaurável campo-a-campo
    }
}
