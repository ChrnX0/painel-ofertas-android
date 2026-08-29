package br.com.painelofertas.usb

import br.com.painelofertas.net.PanelLink
import br.com.painelofertas.net.PanelMessage
import br.com.painelofertas.net.PanelPacket
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Configura o módulo WiFi ESP-AT do painel via USB — porte de Button6Click
 * (ler), Button7Click (entrar na rede) e do state machine de HIDCtrlDeviceData.
 *
 * Portas fixas: o módulo escuta UDP na 17065 e encaminha para o app na 17066.
 * Precisa de validação contra o dispositivo real (respostas do firmware variam).
 */
class WifiModuleConfigurator(private val link: PanelLink) {

    data class ModuleConfig(
        val currentSsid: String = "",
        val ip: String = "",
        val netmask: String = "",
        val gateway: String = "",
        val dhcp: Boolean = true,
        val ssids: List<String> = emptyList(),
    )

    /** Identidade do módulo Wi-Fi (consulta read-only). */
    data class DeviceProbe(
        val firmware: List<String> = emptyList(),  // linhas do AT+GMR
        val mac: String = "",
        val ip: String = "",
    )

    /** Lê a configuração atual e escaneia as redes disponíveis. */
    suspend fun readConfig(): ModuleConfig = coroutineScope {
        val inbox = Channel<PanelMessage>(Channel.UNLIMITED)
        val col = launch { link.incoming.collect { inbox.trySend(it) } }
        try {
            var cfg = ModuleConfig()
            val ssids = linkedSetOf<String>()

            link.sendText("EXIT"); delay(150)

            suspend fun query(cmd: String, collectMs: Long) {
                link.sendText(cmd)
                withTimeoutOrNull(collectMs) {
                    for (m in inbox) {
                        when (m) {
                            is PanelMessage.AtField -> cfg = apply(cfg, m)
                            is PanelMessage.Ok -> return@withTimeoutOrNull
                            is PanelMessage.Text -> extractSsid(m.value)?.let { ssids.add(it) }
                            else -> {}
                        }
                    }
                }
            }

            query("CMD=AT+CIPSTA?", 2000)
            query("CMD=AT+CWDHCP?", 2000)
            query("CMD=AT+CWJAP?", 2000)

            // scan de redes: coleta até FIM_SSID (ou timeout)
            link.sendText("CMD=AT+CWLAP")
            withTimeoutOrNull(SCAN_TIMEOUT) {
                for (m in inbox) {
                    when (m) {
                        is PanelMessage.EndSsid -> return@withTimeoutOrNull
                        is PanelMessage.Text -> extractSsid(m.value)?.let { ssids.add(it) }
                        is PanelMessage.AtField -> if (m.key == "CWJAP") cfg = cfg.copy(currentSsid = m.value)
                        else -> {}
                    }
                }
            }
            cfg.copy(ssids = ssids.toList())
        } finally {
            col.cancel(); inbox.close()
        }
    }

    /**
     * Consulta de identidade (read-only): versão de firmware (AT+GMR) e MAC/IP
     * (AT+CIFSR). Não altera nada no módulo. Precisa de USB conectado.
     */
    suspend fun probe(): DeviceProbe = coroutineScope {
        val inbox = Channel<PanelMessage>(Channel.UNLIMITED)
        val col = launch { link.incoming.collect { inbox.trySend(it) } }
        try {
            val fw = mutableListOf<String>()
            var mac = ""
            var ip = ""
            link.sendText("EXIT"); delay(150)

            link.sendText("CMD=AT+GMR")
            withTimeoutOrNull(PROBE_TIMEOUT) {
                for (m in inbox) when (m) {
                    is PanelMessage.Ok -> return@withTimeoutOrNull
                    is PanelMessage.Text -> {
                        val t = m.value.trim()
                        if (t.isNotEmpty() && t != "AT+GMR" && t != "OK" && !t.startsWith("CMD=")) fw.add(t)
                    }
                    else -> {}
                }
            }

            link.sendText("CMD=AT+CIFSR")
            withTimeoutOrNull(PROBE_TIMEOUT) {
                for (m in inbox) when (m) {
                    is PanelMessage.Ok -> return@withTimeoutOrNull
                    is PanelMessage.Text -> {
                        val t = m.value
                        if (t.contains("STAMAC", ignoreCase = true)) betweenQuotes(t)?.let { mac = it }
                        if (t.contains("STAIP", ignoreCase = true)) betweenQuotes(t)?.let { ip = it }
                    }
                    else -> {}
                }
            }
            DeviceProbe(fw, mac, ip)
        } finally {
            col.cancel(); inbox.close()
        }
    }

    /**
     * Faz o módulo entrar na rede [ssid]/[password] e encaminhar UDP para [localIp].
     * Cada passo espera "OK". Retorna true em sucesso.
     */
    suspend fun join(
        ssid: String,
        password: String,
        localIp: String,
        dhcp: Boolean,
        staticIp: String = "",
        gateway: String = "",
        netmask: String = "",
    ): Boolean = coroutineScope {
        val inbox = Channel<PanelMessage>(Channel.UNLIMITED)
        val col = launch { link.incoming.collect { inbox.trySend(it) } }
        try {
            link.sendText("EXIT"); delay(150)
            if (!step(inbox, "CMD=AT+CWMODE=1")) return@coroutineScope false
            if (!step(inbox, "CMD=AT+CWJAP=\"$ssid\",\"$password\"", JOIN_TIMEOUT)) return@coroutineScope false
            val ipCmd =
                if (dhcp) "CMD=AT+CWDHCP=1,1"
                else "CMD=AT+CIPSTA=\"$staticIp\",\"$gateway\",\"$netmask\""
            if (!step(inbox, ipCmd)) return@coroutineScope false
            if (!step(inbox, "CMD=AT+CIPMUX=0")) return@coroutineScope false
            if (!step(inbox, "CMD=AT+CIPMODE=1")) return@coroutineScope false
            if (!step(inbox, "CMD=AT+SAVETRANSLINK=1,\"$localIp\",17066,\"UDP\",17065")) return@coroutineScope false
            link.sendText("CMD=AT+CIPSEND")
            true
        } finally {
            col.cancel(); inbox.close()
        }
    }

    /**
     * Grava/troca/desliga a senha de transmissão gravada NO painel. [senha] = número
     * (0..4294967295) ou null para desligar. Porte de BitBtn10Click: o painel APAGA a
     * memória ao trocar a senha. Sem ACK documentado no original — retorna após enviar.
     */
    suspend fun setPassword(senha: Long?) {
        link.sendPassword(PanelPacket.passwordBytes(senha))
        delay(200)
    }

    private suspend fun step(inbox: ReceiveChannel<PanelMessage>, cmd: String, timeout: Long = STEP_TIMEOUT): Boolean {
        link.sendText(cmd)
        val m = withTimeoutOrNull(timeout) {
            for (x in inbox) if (x is PanelMessage.Ok || x is PanelMessage.Fail || x is PanelMessage.Err) {
                return@withTimeoutOrNull x
            }
            null
        }
        return m is PanelMessage.Ok
    }

    private fun apply(cfg: ModuleConfig, f: PanelMessage.AtField): ModuleConfig = when (f.key) {
        "ip" -> cfg.copy(ip = f.value)
        "netmask" -> cfg.copy(netmask = f.value)
        "gateway" -> cfg.copy(gateway = f.value)
        "CWJAP" -> cfg.copy(currentSsid = f.value)
        "CWDHCP" -> cfg.copy(dhcp = f.value.trim().let { it == "2" || it == "3" })
        else -> cfg
    }

    /** Conteúdo entre a primeira e a segunda aspa dupla (ex.: +CIFSR:STAMAC,"aa:bb:cc"). */
    private fun betweenQuotes(s: String): String? {
        val a = s.indexOf('"')
        if (a < 0) return null
        val b = s.indexOf('"', a + 1)
        return if (b > a) s.substring(a + 1, b) else null
    }

    /** Extrai o nome da rede de uma linha (+CWLAP:(...,"nome",...) ou nome puro). */
    private fun extractSsid(line: String): String? {
        val t = line.trim()
        if (t.isEmpty() || t == "AT+CWLAP" || t == "OK") return null
        val a = t.indexOf('"')
        if (a >= 0) {
            val b = t.indexOf('"', a + 1)
            if (b > a) return t.substring(a + 1, b)
        }
        return if (t.startsWith("+")) null else t
    }

    companion object {
        const val STEP_TIMEOUT = 8000L
        const val JOIN_TIMEOUT = 15000L
        const val SCAN_TIMEOUT = 8000L
        const val PROBE_TIMEOUT = 2500L
    }
}
