package br.com.painelofertas.net

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Canal de comunicação com UM painel, independente do transporte (UDP ou USB).
 * O [TransferEngine] e o configurador trabalham sobre esta interface.
 */
interface PanelLink {
    val incoming: Flow<PanelMessage>
    suspend fun sendText(cmd: String)
    suspend fun sendErase(codigo: IntArray)
    suspend fun sendDataBlock(offset: Int, chunk: IntArray)
}

/** Link via rede (UDP) para o painel em [ip], usando o [UdpNetwork] compartilhado. */
class UdpLink(
    private val ip: String,
    private val network: UdpNetwork,
) : PanelLink {

    override val incoming: Flow<PanelMessage> =
        network.incoming.filter { it.peerIp == ip }.map { it.message }

    override suspend fun sendText(cmd: String) {
        network.send(ip, PanelPacket.text(cmd))
    }

    override suspend fun sendErase(codigo: IntArray) {
        network.send(ip, PanelPacket.erase(codigo))
    }

    override suspend fun sendDataBlock(offset: Int, chunk: IntArray) {
        network.send(ip, PanelPacket.dataBlock(offset, chunk))
    }
}
