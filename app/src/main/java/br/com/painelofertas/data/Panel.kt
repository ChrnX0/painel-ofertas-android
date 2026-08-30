package br.com.painelofertas.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Estado de conexão de um painel (cores do Shape1 no original). */
enum class PanelStatus { ONLINE, DEGRADED, OFFLINE, USB }

/** O painel exibe exatamente o álbum que a gente espera? (comparação de CRC). */
enum class SyncState { SYNCED, OUTDATED, UNKNOWN }

/** Um painel de LED conhecido/descoberto. */
data class Panel(
    val id: String,
    val name: String,
    val ip: String,
    val freeMemory: Int = 0,
    /** CRC do conteúdo atualmente gravado no painel (reportado no STATUS). */
    val crcPanel: Int = 0,
    /** Brilho-alvo definido pelo usuário (0..100). */
    val brightness: Int = 100,
    /** Leitura ao vivo do sensor/brilho atual (STATUS, campo 7). */
    val intensity: Int = 100,
    /** Auto-brilho pelo sensor de luz (bit 128 no byte de brilho). */
    val sensorAuto: Boolean = false,
    /** CRC do álbum que deveria estar no painel (0 = desconhecido). */
    val expectedCrc: Int = 0,
    /** Instante da última vez visto (epoch ms). 0 = nunca nesta instalação. */
    val lastSeen: Long = 0,
    /** Apelido/observação livre do lojista (ex.: "vitrine", "corredor 3"). */
    val note: String = "",
    /** Grupo/setor do painel (ex.: "Açougue") — permite enviar a todos de uma vez. */
    val group: String = "",
    /** Rede que este painel deve usar (lembrada por painel, editável no card). */
    val ssid: String = "",
    val wifiPassword: String = "",
    val dhcp: Boolean = true,
    val staticIp: String = "",
    val gateway: String = "",
    val netmask: String = "",
    val status: PanelStatus = PanelStatus.OFFLINE,
    val missedBeats: Int = 0,
) {
    val syncState: SyncState
        get() = when {
            expectedCrc == 0 -> SyncState.UNKNOWN
            crcPanel == expectedCrc -> SyncState.SYNCED
            else -> SyncState.OUTDATED
        }
}

/**
 * Persistência dos painéis conhecidos — o "histórico do que foi pareado", que
 * sobrevive a fechar o app. Guarda só os campos estáveis (nome, IP, brilho,
 * sensor, CRC esperado, última vez visto); status/telemetria são recalculados.
 */
interface PanelStore {
    fun load(): List<Panel>
    fun save(panels: List<Panel>)
}

/**
 * Registro dos painéis conhecidos, atualizado pela descoberta/liveness e
 * **persistido** via [store]. Substitui o array global `PainelLB[]` do original
 * por um StateFlow observável que lembra os painéis entre sessões.
 */
class PanelRepository(private val store: PanelStore? = null) {

    private val _panels = MutableStateFlow(store?.load() ?: emptyList())
    val panels: StateFlow<List<Panel>> = _panels

    /** Muda o estado e persiste (para mudanças que valem lembrar). */
    private fun persistUpdate(block: (List<Panel>) -> List<Panel>) {
        _panels.update(block)
        store?.save(_panels.value)
    }

    private fun now(): Long = System.currentTimeMillis()

    /** Cria/atualiza um painel a partir de um STATUS= recebido. */
    fun upsertFromStatus(id: String, ip: String, freeMemory: Int, crc: Int, intensity: Int) {
        persistUpdate { list ->
            val existing = list.firstOrNull { it.id == id }
            val updated = (existing ?: Panel(id = id, name = "Painel $id", ip = ip, brightness = intensity)).copy(
                ip = ip,
                freeMemory = freeMemory,
                crcPanel = crc,
                intensity = intensity,
                status = PanelStatus.ONLINE,
                missedBeats = 0,
                lastSeen = now(),
            )
            if (existing == null) list + updated
            else list.map { if (it.id == id) updated else it }
        }
    }

    /** Marca um painel como visto (reset do contador de falhas). */
    fun markSeen(ip: String) {
        persistUpdate { list -> list.map { if (it.ip == ip) it.copy(missedBeats = 0, status = PanelStatus.ONLINE, lastSeen = now()) else it } }
    }

    /**
     * Incrementa o contador de falhas de todos e reclassifica online/degradado/
     * offline. NÃO persiste (status é derivado; evita gravar a cada 3 s).
     */
    fun tickLiveness() {
        _panels.update { list ->
            list.map { p ->
                val m = p.missedBeats + 1
                val st = when {
                    m > SINAL_VERMELHO -> PanelStatus.OFFLINE
                    m > SINAL_AMARELO -> PanelStatus.DEGRADED
                    else -> p.status
                }
                p.copy(missedBeats = m, status = st)
            }
        }
    }

    fun rename(id: String, newName: String) {
        persistUpdate { list -> list.map { if (it.id == id) it.copy(name = newName) else it } }
    }

    fun setBrightness(id: String, value: Int) {
        persistUpdate { list -> list.map { if (it.id == id) it.copy(brightness = value) else it } }
    }

    fun setSensorAuto(id: String, on: Boolean) {
        persistUpdate { list -> list.map { if (it.id == id) it.copy(sensorAuto = on) else it } }
    }

    /** Marca qual CRC esperamos que o painel (por IP) esteja exibindo — chamado após um envio. */
    fun setExpectedCrc(ip: String, crc: Int) {
        persistUpdate { list -> list.map { if (it.ip == ip) it.copy(expectedCrc = crc) else it } }
    }

    /** Salva a configuração editável do painel: apelido, grupo e rede própria. */
    fun setConfig(
        id: String, note: String, group: String, ssid: String, wifiPassword: String,
        dhcp: Boolean, staticIp: String, gateway: String, netmask: String,
    ) {
        persistUpdate { list ->
            list.map {
                if (it.id == id) it.copy(
                    note = note, group = group, ssid = ssid, wifiPassword = wifiPassword,
                    dhcp = dhcp, staticIp = staticIp, gateway = gateway, netmask = netmask,
                ) else it
            }
        }
    }

    /**
     * Registra/atualiza um painel lido por **etiqueta NFC**. Preenche só o que a
     * etiqueta trouxer, preservando o que já se sabe (brilho, sensor, CRC…).
     */
    fun upsertFromTag(id: String, nome: String, ip: String, grupo: String) {
        if (id.isBlank() && ip.isBlank()) return
        persistUpdate { list ->
            val existente = list.firstOrNull { (id.isNotBlank() && it.id == id) || (ip.isNotBlank() && it.ip == ip) }
            val atualizado = (existente ?: Panel(id = id.ifBlank { ip }, name = nome.ifBlank { "Painel $id" }, ip = ip))
                .copy(
                    name = nome.ifBlank { existente?.name ?: "Painel $id" },
                    ip = ip.ifBlank { existente?.ip ?: "" },
                    group = grupo.ifBlank { existente?.group ?: "" },
                )
            if (existente == null) list + atualizado
            else list.map { if (it === existente) atualizado else it }
        }
    }

    /** Grupos existentes (ex.: Açougue, Hortifruti), em ordem alfabética. */
    fun groups(): List<String> =
        _panels.value.map { it.group.trim() }.filter { it.isNotBlank() }.distinct().sorted()

    /** IPs dos painéis de um grupo — destino do envio em massa. */
    fun ipsOfGroup(group: String): List<String> =
        _panels.value.filter { it.group.trim().equals(group.trim(), ignoreCase = true) }.map { it.ip }

    fun remove(id: String) {
        persistUpdate { list -> list.filterNot { it.id == id } }
    }

    companion object {
        const val SINAL_AMARELO = 5
        const val SINAL_VERMELHO = 10
    }
}
