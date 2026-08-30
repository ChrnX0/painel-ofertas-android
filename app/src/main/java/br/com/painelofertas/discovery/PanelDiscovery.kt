package br.com.painelofertas.discovery

import br.com.painelofertas.data.PanelRepository
import br.com.painelofertas.data.PanelStatus
import br.com.painelofertas.data.SettingsStore
import br.com.painelofertas.net.Datagram
import br.com.painelofertas.net.LocalIp
import br.com.painelofertas.net.PanelMessage
import br.com.painelofertas.net.PanelPacket
import br.com.painelofertas.net.UdpNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * Descoberta e monitoramento de painéis na rede. Escuta os anúncios (STATUS=,
 * CONECTADO, ONLINE), atualiza o [PanelRepository], responde ao painel, e faz
 * a varredura da sub-rede (SERVIDOR=) + o "liveness" periódico.
 * Porte de IdUDPServer1UDPRead + Timer2/Timer3 (Ofertas.pas).
 */
class PanelDiscovery(
    private val udp: UdpNetwork,
    private val panels: PanelRepository,
    private val scope: CoroutineScope,
    private val settings: SettingsStore,
) {
    private val _scanning = MutableStateFlow(false)
    /** Uma varredura está em curso? (alimenta a bolinha amarela "procurando"). */
    val scanning: StateFlow<Boolean> = _scanning

    private val scanLock = Mutex()

    fun start() {
        scope.launch { udp.incoming.collect { runCatching { handle(it) } } }
        scope.launch {
            while (true) {
                delay(TICK_MS)
                panels.tickLiveness()
                pollDegraded()
            }
        }
        // Re-varredura automática enquanto nenhum painel respondeu: se o app abre
        // antes do painel ligar (ou logo depois), ele é encontrado em segundos —
        // sem o usuário precisar tocar em "Procurar".
        scope.launch {
            while (true) {
                delay(RESCAN_MS)
                val online = panels.panels.value.any { it.status == PanelStatus.ONLINE }
                if (!online) {
                    // detecção PRIMEIRO (a rede pode ter mudado); salvo só como último recurso
                    val ip = LocalIp.detect() ?: settings.localIp
                    if (ip.isNotBlank()) runCatching { scan(ip) }
                }
            }
        }
    }

    private suspend fun handle(dg: Datagram) {
        when (val m = dg.message) {
            is PanelMessage.Status -> {
                panels.upsertFromStatus(m.id, dg.peerIp, m.memoriaLivre, m.crc, m.intensidade)
                udp.send(dg.peerIp, PanelPacket.text("ONLINE=${settings.effectMode}"))
            }
            PanelMessage.Connected -> panels.markSeen(dg.peerIp)
            PanelMessage.Online -> {
                panels.markSeen(dg.peerIp)
                udp.send(dg.peerIp, PanelPacket.text("ONLINE"))
            }
            else -> { /* respostas de transferência são tratadas pelo TransferEngine */ }
        }
    }

    private suspend fun pollDegraded() {
        panels.panels.value
            .filter { it.status == PanelStatus.DEGRADED }
            .forEach { udp.send(it.ip, PanelPacket.text("STATUS")) }
    }

    /**
     * Varre a sub-rede /24 do [localIp] anunciando o servidor (SERVIDOR=). Ao
     * contrário do original (5 lotes com 10s de intervalo = até 40s), manda tudo
     * em rajadas curtas: a /24 inteira sai em ~200ms, então o painel responde
     * quase de imediato. Serializada por [scanLock] para não sobrepor varreduras.
     */
    suspend fun scan(localIp: String) {
        val prefix = localIp.substringBeforeLast('.', "")
        if (prefix.isEmpty()) return
        if (!scanLock.tryLock()) return // já há uma varredura em andamento
        _scanning.value = true
        try {
            for (chunk in (1..254).chunked(SCAN_CHUNK)) {
                for (host in chunk) udp.send("$prefix.$host", PanelPacket.text("SERVIDOR=$localIp"))
                delay(SCAN_CHUNK_MS)
            }
            delay(SCAN_SETTLE_MS) // deixa as respostas chegarem antes de baixar "procurando"
        } finally {
            _scanning.value = false
            scanLock.unlock()
        }
    }

    companion object {
        const val TICK_MS = 3000L
        const val RESCAN_MS = 8000L
        const val SCAN_CHUNK = 32
        const val SCAN_CHUNK_MS = 25L
        const val SCAN_SETTLE_MS = 1200L
    }
}
