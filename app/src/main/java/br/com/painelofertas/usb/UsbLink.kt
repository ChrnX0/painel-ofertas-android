package br.com.painelofertas.usb

import br.com.painelofertas.net.PanelLink
import br.com.painelofertas.net.PanelMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [PanelLink] sobre USB HID. Constrói os reports de 64 bytes e interpreta os
 * reports recebidos — porte de EnviarUSB / Timer4 (USB) / HIDCtrlDeviceData.
 *
 * Diferenças em relação ao UDP:
 *  - apagar = report de texto "APAGAR" (sem código de senha);
 *  - bloco de dados = report CMD_TRANSFER (sem prefixo "DADO=");
 *  - offset recebido é **big-endian** (Buf[0]<<8 | Buf[1]).
 */
class UsbLink(
    private val conn: UsbHidConnection,
    scope: CoroutineScope,
) : PanelLink {

    private val _incoming = MutableSharedFlow<PanelMessage>(extraBufferCapacity = 256)
    override val incoming: SharedFlow<PanelMessage> = _incoming.asSharedFlow()

    private val reader: Job = scope.launch(Dispatchers.IO) {
        while (isActive) {
            val body = conn.read(200)
            if (body == null) { delay(20); continue } // evita loop em CPU 100% se não há endpoint IN
            parse(body)?.let { _incoming.tryEmit(it) }
        }
    }

    override suspend fun sendText(cmd: String) = withContext(Dispatchers.IO) {
        conn.send(textReport(cmd)); Unit
    }

    override suspend fun sendErase(codigo: IntArray) = withContext(Dispatchers.IO) {
        conn.send(textReport("APAGAR")); Unit // USB não usa o código no apagar
    }

    override suspend fun sendDataBlock(offset: Int, chunk: IntArray) = withContext(Dispatchers.IO) {
        conn.send(dataReport(offset, chunk)); Unit
    }

    fun close() {
        reader.cancel()
        conn.close() // libera a interface e o UsbDeviceConnection (evita vazamento a cada replug)
    }

    // ---- construção dos reports (corpo de 64 bytes, sem o ReportID) ----

    /** Report de texto: [seq][seq][CMD_PADRAO=1][crc=0][ascii...][13], resto 0. */
    private fun textReport(cmd: String): ByteArray {
        val body = ByteArray(UsbHidManager.REPORT_SIZE)
        body[2] = CMD_PADRAO.toByte()
        var i = 4
        for (c in cmd) { if (i >= body.size - 1) break; body[i++] = (c.code and 0xFF).toByte() }
        body[i] = 13
        return body
    }

    /** Report de bloco: preenche 0xFF, [offLo][offHi][CMD_TRANSFER=2][30][até 60 bytes]. */
    private fun dataReport(offset: Int, chunk: IntArray): ByteArray {
        val body = ByteArray(UsbHidManager.REPORT_SIZE) { 0xFF.toByte() }
        body[0] = (offset and 0xFF).toByte()
        body[1] = ((offset shr 8) and 0xFF).toByte()
        body[2] = CMD_TRANSFER.toByte()
        body[3] = N_DATA.toByte()
        for (k in chunk.indices) { if (4 + k >= body.size) break; body[4 + k] = (chunk[k] and 0xFF).toByte() }
        return body
    }

    private fun parse(body: ByteArray): PanelMessage? {
        if (body.size < 4) return null
        val comando = body[2].toInt() and 0xFF
        if (comando == CMD_TRANSFER) {
            val offset = ((body[0].toInt() and 0xFF) shl 8) or (body[1].toInt() and 0xFF) // big-endian
            val data = IntArray(body.size - 4) { body[4 + it].toInt() and 0xFF }
            return PanelMessage.DataBlock(offset, data)
        }
        // resposta de texto a partir do índice 4, até NUL/CR
        val sb = StringBuilder()
        var i = 4
        while (i < body.size) {
            val b = body[i].toInt() and 0xFF
            if (b == 0 || b == 13) break
            sb.append(b.toChar())
            i++
        }
        val text = sb.toString().trim()
        return if (text.isEmpty()) null else PanelMessage.parse(text)
    }

    companion object {
        const val CMD_PADRAO = 1
        const val CMD_TRANSFER = 2
        const val N_DATA = 30
    }
}
