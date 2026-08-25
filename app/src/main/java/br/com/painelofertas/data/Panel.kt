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
 * Registro em memória dos painéis conhecidos, atualizado pela descoberta/liveness.
 * Substitui o array global `PainelLB[]` do original por um StateFlow observável.
 */
class PanelRepository {

    private val _panels = MutableStateFlow<List<Panel>>(emptyList())
    val panels: StateFlow<List<Panel>> = _panels

    /** Cria/atualiza um painel a partir de um STATUS= recebido. */
    fun upsertFromStatus(id: String, ip: String, freeMemory: Int, crc: Int, intensity: Int) {
        _panels.update { list ->
            val existing = list.firstOrNull { it.id == id }
            val updated = (existing ?: Panel(id = id, name = "Painel $id", ip = ip, brightness = intensity)).copy(
                ip = ip,
                freeMemory = freeMemory,
                crcPanel = crc,
                intensity = intensity,
                status = PanelStatus.ONLINE,
                missedBeats = 0,
            )
            if (existing == null) list + updated
            else list.map { if (it.id == id) updated else it }
        }
    }

    /** Marca um painel como visto (reset do contador de falhas). */
    fun markSeen(ip: String) {
        _panels.update { list -> list.map { if (it.ip == ip) it.copy(missedBeats = 0, status = PanelStatus.ONLINE) else it } }
    }

    /** Incrementa o contador de falhas de todos e reclassifica online/degradado/offline. */
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
        _panels.update { list -> list.map { if (it.id == id) it.copy(name = newName) else it } }
    }

    fun setBrightness(id: String, value: Int) {
        _panels.update { list -> list.map { if (it.id == id) it.copy(brightness = value) else it } }
    }

    fun setSensorAuto(id: String, on: Boolean) {
        _panels.update { list -> list.map { if (it.id == id) it.copy(sensorAuto = on) else it } }
    }

    /** Marca qual CRC esperamos que o painel (por IP) esteja exibindo — chamado após um envio. */
    fun setExpectedCrc(ip: String, crc: Int) {
        _panels.update { list -> list.map { if (it.ip == ip) it.copy(expectedCrc = crc) else it } }
    }

    fun remove(id: String) {
        _panels.update { list -> list.filterNot { it.id == id } }
    }

    companion object {
        const val SINAL_AMARELO = 5
        const val SINAL_VERMELHO = 10
    }
}
