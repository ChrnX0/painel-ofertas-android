package br.com.painelofertas

import android.content.Context
import br.com.painelofertas.data.AlbumStore
import br.com.painelofertas.data.Panel
import br.com.painelofertas.data.PairedPanelsStore
import br.com.painelofertas.data.PanelRepository
import br.com.painelofertas.data.ScheduleStore
import br.com.painelofertas.data.SettingsStore
import br.com.painelofertas.discovery.PanelDiscovery
import br.com.painelofertas.net.ConnectionCenter
import br.com.painelofertas.net.DiagnosticsLog
import br.com.painelofertas.net.Encriptor
import br.com.painelofertas.net.LocalIp
import br.com.painelofertas.net.PanelLink
import br.com.painelofertas.net.UdpLink
import br.com.painelofertas.net.UdpNetwork
import br.com.painelofertas.protocol.Album
import br.com.painelofertas.protocol.AlbumCodec
import br.com.painelofertas.protocol.BinaryCodec
import br.com.painelofertas.render.FontRepository
import br.com.painelofertas.transfer.TransferEngine
import br.com.painelofertas.usb.UsbController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * "DI" manual: cria e mantém as dependências de longa vida (rede, USB,
 * repositórios). Uma única instância vive na [PainelApp].
 */
class AppContainer(context: Context) {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings = SettingsStore(context)
    val panels = PanelRepository(PairedPanelsStore(context))
    val albums = AlbumStore(context)
    val schedule = ScheduleStore(context)
    val fonts = FontRepository(context)

    val diag = DiagnosticsLog()
    val udp = UdpNetwork(appScope, log = diag)
    val discovery = PanelDiscovery(udp, panels, appScope, settings)
    val usb = UsbController(context, appScope)
    val connection = ConnectionCenter(appScope, panels, discovery, usb)

    init {
        appScope.launch(Dispatchers.IO) {
            runCatching {
                udp.start()
                discovery.start()
                // Auto-conectar ao abrir: se o celular já está numa rede, anuncia o
                // servidor na sub-rede e os painéis na mesma rede aparecem sozinhos.
                delay(800)
                autoConnect()
            }
        }
        runCatching { usb.start() }
        startScheduler()
    }

    /** Dispara uma varredura da rede local (usado no boot e ao abrir a aba Painéis). */
    fun autoConnect() {
        appScope.launch {
            val ip = settings.localIp.ifBlank { LocalIp.detect() ?: "" }
            if (ip.isNotBlank()) {
                if (settings.localIp.isBlank()) settings.localIp = ip
                runCatching { discovery.scan(ip) }
            }
        }
    }

    /** Link UDP para um painel específico. */
    fun udpLink(panel: Panel): PanelLink = UdpLink(panel.ip, udp)

    fun transfer(link: PanelLink) = TransferEngine(link)

    /**
     * Lê o álbum atualmente gravado no painel (Receber/CARREGAR), para o editor
     * mostrar o que já está lá e o usuário escolher onde inserir a nova tela.
     */
    suspend fun downloadAlbum(link: PanelLink): Album? {
        val r = TransferEngine(link).download() ?: return null
        val blocos = BinaryCodec.decompile(r.bytes)
        return Album(name = "Painel", frames = AlbumCodec.parseFrames(blocos))
    }

    /** Apaga TODA a memória do painel e reativa vazio (botão "Limpar painel"). */
    suspend fun clearPanel(link: PanelLink, brilho: Int): Boolean {
        val codigo =
            if (settings.useTxPassword) Encriptor.code(settings.txPassword, System.currentTimeMillis().toString())
            else IntArray(10)
        return TransferEngine(link).eraseAll(codigo, brilho)
    }

    /** Compila e envia um álbum salvo para um IP (usado pela agenda e por atalhos). */
    suspend fun sendAlbumByName(albumName: String, ip: String, brightness: Int? = null): Boolean {
        val album = albums.load(albumName) ?: return false
        val r = album.compile()
        val codigo =
            if (settings.useTxPassword) Encriptor.code(settings.txPassword, System.currentTimeMillis().toString())
            else IntArray(10)
        val ok = TransferEngine(UdpLink(ip, udp)).upload(r.bytes, codigo, brightness ?: album.brilho)
        if (ok) panels.setExpectedCrc(ip, r.crc)   // agora sabemos qual CRC o painel deveria ter
        return ok
    }

    /**
     * Agendador simples (estilo Timer2 do original): enquanto o app está aberto,
     * verifica a cada minuto se há tarefas para disparar. Agendamento em segundo
     * plano (app fechado) exigiria WorkManager — melhoria futura.
     */
    private fun startScheduler() {
        appScope.launch {
            var lastMinute = -1
            while (true) {
                delay(20_000)
                val c = Calendar.getInstance()
                val minute = c.get(Calendar.MINUTE)
                if (minute == lastMinute) continue
                lastMinute = minute
                val h = c.get(Calendar.HOUR_OF_DAY)
                val d = c.get(Calendar.DAY_OF_MONTH)
                val mo = c.get(Calendar.MONTH) + 1
                val y = c.get(Calendar.YEAR)
                schedule.list().filter { it.dueAt(h, minute, d, mo, y) }.forEach { t ->
                    val brilho = if (t.brightness in 0..100) t.brightness else null
                    runCatching { sendAlbumByName(t.album, t.panelIp, brilho) }
                    if (!t.daily) schedule.remove(t.id) // tarefas diárias permanecem
                }
            }
        }
    }
}
