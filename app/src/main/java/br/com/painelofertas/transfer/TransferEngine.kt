package br.com.painelofertas.transfer

import br.com.painelofertas.net.PanelLink
import br.com.painelofertas.net.PanelMessage
import br.com.painelofertas.net.PanelPacket
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Progresso/desfecho de uma transferência. */
sealed interface TransferProgress {
    data class Uploading(val sent: Int, val total: Int) : TransferProgress
    data class Downloading(val received: Int, val total: Int) : TransferProgress
    data object Done : TransferProgress
    data class Failed(val reason: String) : TransferProgress
}

/** Resultado de um download: os bytes recebidos (para descompilar em `.alb`). */
class DownloadResult(val bytes: IntArray)

/**
 * Envia (Enviar) e recebe (Receber) conteúdo do painel em blocos de 60 bytes,
 * stop-and-wait com retransmissão — porte de BitBtn1Click/BitBtn3Click/Timer4/Timer5
 * (Ofertas.pas). Funciona sobre qualquer [PanelLink] (UDP ou USB).
 */
class TransferEngine(private val link: PanelLink) {

    /**
     * Upload: apaga, envia os blocos confirmando cada um por `NEXT=`, e ativa o
     * conteúdo com `INICIAR=<brilho>`. Retorna true em sucesso.
     */
    suspend fun upload(
        bytes: IntArray,
        codigo: IntArray,
        brilho: Int,
        onProgress: (TransferProgress) -> Unit = {},
    ): Boolean = coroutineScope {
        val inbox = Channel<PanelMessage>(Channel.UNLIMITED)
        val collector = launch { link.incoming.collect { inbox.trySend(it) } }
        try {
            // 1) apaga e espera "APAGADO"
            var erased = false
            for (attempt in 1..ERASE_ATTEMPTS) {
                link.sendErase(codigo)
                if (waitFor(inbox, ERASE_TIMEOUT) { it is PanelMessage.Erased } != null) {
                    erased = true; break
                }
            }
            if (!erased) { onProgress(TransferProgress.Failed("Painel não respondeu ao apagar")); return@coroutineScope false }

            // 2) envia blocos (stop-and-wait, ack exato por NEXT=offset)
            var offset = 0
            while (offset < bytes.size) {
                val chunk = PanelPacket.chunkAt(bytes, offset)
                var acked = false
                for (attempt in 1..BLOCK_ATTEMPTS) {
                    link.sendDataBlock(offset, chunk)
                    onProgress(TransferProgress.Uploading(offset, bytes.size))
                    val ack = waitFor(inbox, BLOCK_TIMEOUT) { it is PanelMessage.Next && it.offset == offset }
                    if (ack != null) { acked = true; break }
                }
                if (!acked) { onProgress(TransferProgress.Failed("Falha no bloco $offset")); return@coroutineScope false }
                offset += PanelPacket.BLOCK_SIZE
            }

            // 3) ativa o conteúdo
            link.sendText("INICIAR=$brilho")
            onProgress(TransferProgress.Done)
            true
        } finally {
            collector.cancel()
            inbox.close()
        }
    }

    /**
     * Download: pede `CARREGAR`, recebe o tamanho por `MEMORIA=`, coleta os blocos
     * confirmando cada um por `LIDO=`, até `ARQUIVAR`. Retorna os bytes recebidos.
     */
    suspend fun download(onProgress: (TransferProgress) -> Unit = {}): DownloadResult? = coroutineScope {
        val inbox = Channel<PanelMessage>(Channel.UNLIMITED)
        val collector = launch { link.incoming.collect { inbox.trySend(it) } }
        try {
            link.sendText("CARREGAR")
            val mem = waitFor(inbox, MEMORY_TIMEOUT) { it is PanelMessage.Memory } as? PanelMessage.Memory
                ?: run { onProgress(TransferProgress.Failed("Sem resposta MEMORIA")); return@coroutineScope null }
            if (mem.size <= 0 || mem.size > MAX_DOWNLOAD_BYTES) {
                onProgress(TransferProgress.Failed("Tamanho de memória inválido (${mem.size})"))
                return@coroutineScope null
            }

            val rx = IntArray(mem.size + 70) { 255 }
            link.sendText("LIDO=0")

            while (true) {
                val m = waitFor(inbox, DOWNLOAD_WATCHDOG) {
                    it is PanelMessage.DataBlock || it is PanelMessage.Archive
                } ?: run { onProgress(TransferProgress.Failed("Download expirou")); return@coroutineScope null }

                when (m) {
                    is PanelMessage.DataBlock -> {
                        val count = minOf(PanelPacket.BLOCK_SIZE, m.data.size)
                        for (i in 0 until count) {
                            val idx = m.offset + i
                            if (idx in rx.indices) rx[idx] = m.data[i]
                        }
                        link.sendText("LIDO=${m.offset + PanelPacket.BLOCK_SIZE}")
                        onProgress(TransferProgress.Downloading(m.offset, mem.size))
                    }
                    is PanelMessage.Archive -> {
                        onProgress(TransferProgress.Done)
                        return@coroutineScope DownloadResult(rx)
                    }
                    else -> {}
                }
            }
            @Suppress("UNREACHABLE_CODE")
            null
        } finally {
            collector.cancel()
            inbox.close()
        }
    }

    /**
     * Limpa toda a memória do painel: apaga (esperando `APAGADO`) e reativa vazio
     * com `INICIAR=<brilho>`, sem enviar nenhum bloco. Recurso novo (o original só
     * apagava como 1º passo de um envio).
     */
    suspend fun eraseAll(codigo: IntArray, brilho: Int, onProgress: (TransferProgress) -> Unit = {}): Boolean = coroutineScope {
        val inbox = Channel<PanelMessage>(Channel.UNLIMITED)
        val collector = launch { link.incoming.collect { inbox.trySend(it) } }
        try {
            var erased = false
            for (attempt in 1..ERASE_ATTEMPTS) {
                link.sendErase(codigo)
                if (waitFor(inbox, ERASE_TIMEOUT) { it is PanelMessage.Erased } != null) { erased = true; break }
            }
            if (!erased) { onProgress(TransferProgress.Failed("Painel não respondeu ao apagar")); return@coroutineScope false }
            link.sendText("INICIAR=$brilho")
            onProgress(TransferProgress.Done)
            true
        } finally {
            collector.cancel()
            inbox.close()
        }
    }

    private suspend fun waitFor(
        inbox: ReceiveChannel<PanelMessage>,
        timeoutMs: Long,
        predicate: (PanelMessage) -> Boolean,
    ): PanelMessage? = withTimeoutOrNull(timeoutMs) {
        for (m in inbox) if (predicate(m)) return@withTimeoutOrNull m
        null
    }

    companion object {
        const val ERASE_ATTEMPTS = 3
        const val ERASE_TIMEOUT = 3000L
        const val BLOCK_ATTEMPTS = 50    // "insistencia" do original
        const val BLOCK_TIMEOUT = 1000L
        const val MEMORY_TIMEOUT = 5000L
        const val DOWNLOAD_WATCHDOG = 3000L
        const val MAX_DOWNLOAD_BYTES = 2_000_000 // teto de sanidade contra pacote MEMORIA= malformado
    }
}
