package br.com.painelofertas.nfc

/**
 * Etiqueta NFC de um painel — preparação para os modelos com essa tecnologia.
 *
 * A ideia: encostar o celular no painel e o app já sabe **qual painel é** (id,
 * rede, grupo), sem procurar na lista nem digitar IP. Instalação e manutenção em
 * loja com muitos painéis deixam de depender de decorar endereços.
 *
 * ## Formato gravado na etiqueta
 * Um registro NDEF de **texto** (ou URI `ledblock://painel?...`) com pares
 * `chave=valor` separados por `;`:
 * ```
 * lb=1;id=07;nome=Vitrine da Frente;ip=192.168.0.42;grupo=Acougue;modelo=LB-96;fw=1.2
 * ```
 * - `lb=1` identifica que a etiqueta é de um painel LedBlock (obrigatório)
 * - `id` é o identificador do painel (o mesmo do `STATUS=`)
 * - o resto é opcional: o app usa o que houver
 *
 * O formato é **tolerante**: chaves desconhecidas são ignoradas, a ordem não
 * importa e espaços são aparados — assim etiquetas gravadas por versões futuras
 * continuam sendo lidas por esta.
 */
data class PanelTag(
    val id: String = "",
    val nome: String = "",
    val ip: String = "",
    val grupo: String = "",
    val modelo: String = "",
    val firmware: String = "",
) {
    val temAlgo: Boolean get() = id.isNotBlank() || ip.isNotBlank() || nome.isNotBlank()

    /** Texto para gravar numa etiqueta nova (uso da LedBlock na fábrica/instalação). */
    fun toTagText(): String = buildString {
        append("lb=1")
        if (id.isNotBlank()) append(";id=$id")
        if (nome.isNotBlank()) append(";nome=$nome")
        if (ip.isNotBlank()) append(";ip=$ip")
        if (grupo.isNotBlank()) append(";grupo=$grupo")
        if (modelo.isNotBlank()) append(";modelo=$modelo")
        if (firmware.isNotBlank()) append(";fw=$firmware")
    }

    companion object {
        /**
         * Interpreta o conteúdo de uma etiqueta. Devolve null se não for de um
         * painel LedBlock — assim o app não reage a crachás, cartões e etiquetas
         * de outros sistemas que o celular encostar por acaso.
         */
        fun parse(conteudo: String): PanelTag? {
            val limpo = conteudo.trim()
                .removePrefix("ledblock://painel?")
                .removePrefix("ledblock://")
            val pares = limpo.split(';', '&')
                .mapNotNull { par ->
                    val i = par.indexOf('=')
                    if (i <= 0) null else par.substring(0, i).trim().lowercase() to par.substring(i + 1).trim()
                }.toMap()

            // Só aceita etiqueta declaradamente LedBlock.
            if (pares["lb"] != "1") return null

            val tag = PanelTag(
                id = pares["id"].orEmpty(),
                nome = pares["nome"].orEmpty(),
                ip = pares["ip"].orEmpty(),
                grupo = pares["grupo"].orEmpty(),
                modelo = pares["modelo"].orEmpty(),
                firmware = pares["fw"] ?: pares["firmware"].orEmpty(),
            )
            return if (tag.temAlgo) tag else null
        }
    }
}
