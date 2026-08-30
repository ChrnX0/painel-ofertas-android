package br.com.painelofertas.ui.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.painelofertas.AppContainer
import br.com.painelofertas.net.Encriptor
import br.com.painelofertas.net.PanelLink
import br.com.painelofertas.protocol.Album
import br.com.painelofertas.protocol.AlbumCodec
import br.com.painelofertas.protocol.BinaryCodec
import br.com.painelofertas.transfer.TransferEngine
import br.com.painelofertas.transfer.TransferProgress
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Envio/recebimento pro painel. Roda no [viewModelScope] (escopo de Activity),
 * então NÃO é cancelado ao trocar de aba — evita deixar o painel pela metade.
 * Expõe [busy]/[progress]/[status] para a UI travar botões e mostrar barra.
 */
class SendViewModel : ViewModel() {

    var busy by mutableStateOf(false); private set
    var status by mutableStateOf("Pronto."); private set
    var progress by mutableStateOf<Float?>(null); private set

    fun enviar(container: AppContainer, albumName: String, link: PanelLink, targetIp: String? = null, viaUsb: Boolean = false) {
        if (busy) return
        val album = container.albums.load(albumName) ?: run { status = "Álbum não encontrado."; return }
        busy = true; progress = 0f; status = "Preparando…"
        container.connection.transferStarted(viaUsb)
        viewModelScope.launch {
            var ok = false
            try {
                val r = album.compile()
                val codigo =
                    if (container.settings.useTxPassword)
                        Encriptor.code(container.settings.txPassword, System.currentTimeMillis().toString())
                    else IntArray(10)
                ok = TransferEngine(link).upload(r.bytes, codigo, album.brilho) { p ->
                    when (p) {
                        is TransferProgress.Uploading -> {
                            progress = if (p.total > 0) p.sent.toFloat() / p.total else null
                            status = "Enviando ${p.sent}/${p.total} bytes…"
                        }
                        is TransferProgress.Done -> { progress = 1f; status = "✅ Enviado com sucesso." }
                        is TransferProgress.Failed -> status = "❌ ${p.reason}"
                        else -> {}
                    }
                }
                if (ok) targetIp?.let { container.panels.setExpectedCrc(it, r.crc) }
                if (!ok) status = "❌ Falha no envio."
            } catch (e: Exception) {
                status = "❌ Erro: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                container.connection.transferEnded(viaUsb, ok)
                busy = false; progress = null
            }
        }
    }

    /**
     * Envia o mesmo álbum para VÁRIOS painéis, um após o outro (o protocolo é
     * stop-and-wait por painel). Relata o progresso de cada um e um resumo no fim —
     * o caso real de quem tem vários painéis na mesma loja.
     */
    fun enviarParaVarios(container: AppContainer, albumName: String, ips: List<String>) {
        if (busy || ips.isEmpty()) return
        val album = container.albums.load(albumName) ?: run { status = "Álbum não encontrado."; return }
        busy = true; progress = 0f
        container.connection.transferStarted(false)
        viewModelScope.launch {
            var okCount = 0
            val falhas = mutableListOf<String>()
            try {
                val r = album.compile()
                ips.forEachIndexed { idx, ip ->
                    status = "Enviando para ${idx + 1} de ${ips.size} (${ip})…"
                    val codigo =
                        if (container.settings.useTxPassword)
                            Encriptor.code(container.settings.txPassword, System.currentTimeMillis().toString())
                        else IntArray(10)
                    val ok = runCatching {
                        TransferEngine(container.udpLinkByIp(ip)).upload(r.bytes, codigo, album.brilho) { p ->
                            if (p is TransferProgress.Uploading && p.total > 0) {
                                val doPainel = p.sent.toFloat() / p.total
                                progress = (idx + doPainel) / ips.size
                            }
                        }
                    }.getOrDefault(false)
                    if (ok) { okCount++; container.panels.setExpectedCrc(ip, r.crc) } else falhas.add(ip)
                }
                status = when {
                    falhas.isEmpty() -> "✅ Enviado para ${okCount} painel(is)."
                    okCount == 0 -> "❌ Falhou em todos os painéis."
                    else -> "⚠ Enviado para $okCount; falhou em: ${falhas.joinToString()}"
                }
            } catch (e: Exception) {
                status = "❌ Erro: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                container.connection.transferEnded(false, falhas.isEmpty())
                busy = false; progress = null
            }
        }
    }

    fun receber(container: AppContainer, link: PanelLink, viaUsb: Boolean = false) {
        if (busy) return
        busy = true; progress = 0f; status = "Recebendo…"
        container.connection.transferStarted(viaUsb)
        viewModelScope.launch {
            var ok = false
            try {
                val result = TransferEngine(link).download { p ->
                    if (p is TransferProgress.Downloading) {
                        progress = if (p.total > 0) p.received.toFloat() / p.total else null
                        status = "Recebendo ${p.received}/${p.total} bytes…"
                    }
                }
                if (result == null) { status = "❌ Falha ao receber."; return@launch }
                val blocos = BinaryCodec.decompile(result.bytes)
                val stamp = SimpleDateFormat("dd-MM HH'h'mm", Locale.getDefault()).format(Date())
                val album = Album(name = "Recebido $stamp", frames = AlbumCodec.parseFrames(blocos))
                container.albums.save(album)
                ok = true
                status = "✅ Recebido e salvo como \"${album.name}\"."
            } catch (e: Exception) {
                status = "❌ Erro: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                container.connection.transferEnded(viaUsb, ok)
                busy = false; progress = null
            }
        }
    }
}
