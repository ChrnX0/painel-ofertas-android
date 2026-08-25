package br.com.painelofertas.net

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

/** Um datagrama recebido: IP de origem + mensagem já interpretada. */
data class Datagram(val peerIp: String, val message: PanelMessage)

/**
 * Gerencia o socket UDP: escuta respostas na porta 17066 e envia para os painéis
 * na 17065. Porte de `IdUDPServer1`/`ClienteUDP` (Ofertas.pas). O offset dos blocos
 * de download é little-endian (`AData[1] + AData[2]<<8`, linha 8332).
 */
class UdpNetwork(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val listenPort: Int = PanelPacket.PORT_LISTEN,
    private val panelPort: Int = PanelPacket.PORT_PANEL,
) {
    private var socket: DatagramSocket? = null
    private var receiver: Job? = null

    private val _incoming = MutableSharedFlow<Datagram>(extraBufferCapacity = 512)
    val incoming: SharedFlow<Datagram> = _incoming.asSharedFlow()

    @Synchronized
    fun start() {
        if (socket != null) return
        val s = DatagramSocket(null).apply {
            reuseAddress = true
            broadcast = true
            bind(InetSocketAddress(listenPort))
        }
        socket = s
        receiver = scope.launch {
            val buf = ByteArray(4096)
            while (isActive) {
                try {
                    val p = DatagramPacket(buf, buf.size)
                    s.receive(p)
                    val ip = p.address?.hostAddress ?: continue
                    _incoming.tryEmit(Datagram(ip, decode(p.data, p.offset, p.length)))
                } catch (_: Exception) {
                    if (!isActive || socket == null) break
                }
            }
        }
    }

    private fun decode(data: ByteArray, offset: Int, length: Int): PanelMessage {
        if (length >= 4 && data[offset].toInt() == 0) {
            val off = (data[offset + 1].toInt() and 0xFF) or ((data[offset + 2].toInt() and 0xFF) shl 8)
            val payload = IntArray(length - 4) { data[offset + 4 + it].toInt() and 0xFF }
            return PanelMessage.DataBlock(off, payload)
        }
        return PanelMessage.parse(String(data, offset, length, Charsets.ISO_8859_1))
    }

    suspend fun send(ip: String, bytes: ByteArray): Unit = withContext(Dispatchers.IO) {
        try {
            socket?.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName(ip), panelPort))
        } catch (_: Exception) {
            // rede indisponível ou IP/host inválido — ignora em vez de derrubar o app
        }
    }

    suspend fun broadcast(bytes: ByteArray): Unit = withContext(Dispatchers.IO) {
        try {
            socket?.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName("255.255.255.255"), panelPort))
        } catch (_: Exception) {
        }
    }

    @Synchronized
    fun stop() {
        receiver?.cancel()
        receiver = null
        socket?.close()
        socket = null
    }
}
