package br.com.painelofertas.render

import android.content.Context
import br.com.painelofertas.protocol.PanelFont

/**
 * Carrega e mantém em cache as 5 fontes `.flb` embutidas em assets/fonts/.
 * Implementa [FontProvider] para ser usada diretamente pelo [PanelRenderer].
 */
class FontRepository(context: Context) : FontProvider {

    private val appContext = context.applicationContext
    private val cache = HashMap<Int, FlbFont>()

    override fun font(code: Int): FlbFont = cache.getOrPut(code) {
        val fileName = PanelFont.of(code).fileName
        val text = appContext.assets.open("fonts/$fileName")
            .use { it.readBytes().toString(Charsets.ISO_8859_1) }
        FlbFont.fromText(text)
    }
}
