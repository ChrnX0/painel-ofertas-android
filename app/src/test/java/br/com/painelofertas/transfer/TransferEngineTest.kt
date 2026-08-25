package br.com.painelofertas.transfer

import br.com.painelofertas.net.PanelLink
import br.com.painelofertas.net.PanelMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Painel simulado: responde ao protocolo como o hardware faria, permitindo
 * validar o [TransferEngine] sem nenhum painel físico.
 */
private class FakePanel(private val downloadData: IntArray = IntArray(0)) : PanelLink {
    private val ch = Channel<PanelMessage>(Channel.UNLIMITED)
    override val incoming = ch.receiveAsFlow()

    val sentBlocks = mutableListOf<Pair<Int, IntArray>>()
    var iniciarBrilho = -1

    override suspend fun sendErase(codigo: IntArray) {
        ch.trySend(PanelMessage.Erased)
    }

    override suspend fun sendDataBlock(offset: Int, chunk: IntArray) {
        sentBlocks.add(offset to chunk)
        ch.trySend(PanelMessage.Next(offset)) // ack exato
    }

    override suspend fun sendText(cmd: String) {
        when {
            cmd == "CARREGAR" -> ch.trySend(PanelMessage.Memory(downloadData.size))
            cmd.startsWith("LIDO=") -> {
                val off = cmd.removePrefix("LIDO=").toInt()
                if (off < downloadData.size) {
                    val end = minOf(off + 60, downloadData.size)
                    ch.trySend(PanelMessage.DataBlock(off, downloadData.copyOfRange(off, end)))
                } else {
                    ch.trySend(PanelMessage.Archive)
                }
            }
            cmd.startsWith("INICIAR=") -> iniciarBrilho = cmd.removePrefix("INICIAR=").toInt()
        }
    }
}

class TransferEngineTest {

    @Test
    fun upload_sends_all_blocks_and_activates() = runTest {
        val fake = FakePanel()
        val bytes = IntArray(130) { it and 0xFF } // 3 blocos: 60 + 60 + 10
        val ok = TransferEngine(fake).upload(bytes, codigo = IntArray(10), brilho = 80)

        assertTrue(ok)
        assertEquals(listOf(0, 60, 120), fake.sentBlocks.map { it.first })
        assertEquals(10, fake.sentBlocks.last().second.size)
        assertEquals(80, fake.iniciarBrilho)
    }

    @Test
    fun download_reassembles_bytes() = runTest {
        val data = IntArray(130) { it and 0xFF }
        val fake = FakePanel(downloadData = data)
        val result = TransferEngine(fake).download()

        assertNotNull(result)
        val rx = result!!.bytes
        for (i in data.indices) assertEquals("byte $i", data[i], rx[i])
    }
}
