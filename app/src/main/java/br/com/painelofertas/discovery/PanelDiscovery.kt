package br.com.painelofertas.discovery

import br.com.painelofertas.data.PanelRepository
import br.com.painelofertas.data.PanelStatus
import br.com.painelofertas.data.SettingsStore
import br.com.painelofertas.net.Datagram
import br.com.painelofertas.net.PanelMessage
import br.com.painelofertas.net.PanelPacket
import br.com.painelofertas.net.UdpNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    fun start() {
        scope.launch { udp.incoming.collect { runCatching { handle(it) } } }
        scope.launch {
            while (true) {
                delay(TICK_MS)
                panels.tickLiveness()
                pollDegraded()
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
     * Varre a sub-rede /24 do [localIp] anunciando o servidor (SERVIDOR=),
     * em 5 lotes de ~50 endereços com 10s entre eles (como Timer3 no original).
     */
    suspend fun scan(localIp: String) {
        val prefix = localIp.substringBeforeLast('.', "")
        if (prefix.isEmpty()) return
        val batches = listOf(1..50, 51..100, 101..150, 151..200, 201..254)
        for ((i, range) in batches.withIndex()) {
            for (host in range) udp.send("$prefix.$host", PanelPacket.text("SERVIDOR=$localIp"))
            if (i < batches.lastIndex) delay(SCAN_BATCH_MS)
        }
    }

    companion object {
        const val TICK_MS = 3000L
        const val SCAN_BATCH_MS = 10000L
    }
}
