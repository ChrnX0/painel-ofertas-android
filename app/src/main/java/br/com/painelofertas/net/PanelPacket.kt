package br.com.painelofertas.net

/**
 * Montagem dos pacotes UDP enviados ao painel — porte fiel de EnviarPckUDP /
 * Timer4Timer (Ofertas.pas). Porta do painel = 17065; respostas chegam na 17066.
 */
object PanelPacket {
    const val PORT_PANEL = 17065
    const val PORT_LISTEN = 17066

    const val CMD_TRANSFER = 2
    private const val N_DATA = 30 // constante fixa no original (não é o tamanho real do bloco)
    const val BLOCK_SIZE = 60

    /**
     * Comando de texto: ASCII + CR(13). Regra do original: se o texto tiver
     * exatamente 76 caracteres, insere um espaço antes do CR.
     */
    fun text(cmd: String): ByteArray {
        val body = if (cmd.length == 76) "$cmd " else cmd
        val out = ByteArray(body.length + 1)
        for (i in body.indices) out[i] = (body[i].code and 0xFF).toByte()
        out[body.length] = 13
        return out
    }

    /**
     * Bloco de dados para upload: `DADO=` + offsetLo + offsetHi + CMD_TRANSFER(2)
     * + 30 + até 60 bytes de conteúdo. Sem CR.
     */
    fun dataBlock(offset: Int, chunk: IntArray): ByteArray {
        val prefix = "DADO=".encodeToByteArray()
        val out = ByteArray(prefix.size + 4 + chunk.size)
        var i = 0
        prefix.forEach { out[i++] = it }
        out[i++] = (offset and 0xFF).toByte()
        out[i++] = ((offset shr 8) and 0xFF).toByte()
        out[i++] = CMD_TRANSFER.toByte()
        out[i++] = N_DATA.toByte()
        for (b in chunk) out[i++] = (b and 0xFF).toByte()
        return out
    }

    /** Comando de apagar antes do upload: `APAGAR=` + 10 bytes de código de acesso. */
    fun erase(codigo: IntArray): ByteArray {
        val prefix = "APAGAR=".encodeToByteArray()
        val out = ByteArray(prefix.size + codigo.size)
        var i = 0
        prefix.forEach { out[i++] = it }
        for (b in codigo) out[i++] = (b and 0xFF).toByte()
        return out
    }

    /**
     * Grava/troca/desliga a senha de transmissão gravada NO painel:
     * `SENHA=` + 4 bytes. Sem CR (como o APAGAR). Porte de BitBtn10Click.
     * O painel apaga a memória ao trocar a senha.
     */
    fun password(codeBytes: IntArray): ByteArray {
        val prefix = "SENHA=".encodeToByteArray()
        val out = ByteArray(prefix.size + codeBytes.size)
        var i = 0
        prefix.forEach { out[i++] = it }
        for (b in codeBytes) out[i++] = (b and 0xFF).toByte()
        return out
    }

    /** Os 4 bytes da senha: little-endian do número, ou 255,255,255,255 para desligar. */
    fun passwordBytes(senha: Long?): IntArray =
        if (senha == null) intArrayOf(255, 255, 255, 255)
        else intArrayOf(
            (senha and 0xFF).toInt(),
            ((senha shr 8) and 0xFF).toInt(),
            ((senha shr 16) and 0xFF).toInt(),
            ((senha shr 24) and 0xFF).toInt(),
        )

    /** Fatia de até 60 bytes começando em [offset] do fluxo completo. */
    fun chunkAt(bytes: IntArray, offset: Int): IntArray {
        val end = minOf(offset + BLOCK_SIZE, bytes.size)
        return if (offset >= bytes.size) IntArray(0) else bytes.copyOfRange(offset, end)
    }
}
