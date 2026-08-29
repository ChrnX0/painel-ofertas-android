package br.com.painelofertas.net

import br.com.painelofertas.data.PanelRepository
import br.com.painelofertas.data.PanelStatus
import br.com.painelofertas.discovery.PanelDiscovery
import br.com.painelofertas.usb.UsbController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Fase de um enlace (Wi-Fi ou USB), traduzida direto para a cor da bolinha na
 * barra superior — o que o lojista pediu para acompanhar o processo:
 *
 * - [OFFLINE]   cinza     — sem rede / nada plugado
 * - [SEARCHING] amarelo   — conectado à rede, procurando o painel
 * - [ONLINE]    verde     — painel encontrado e respondendo
 * - [TRANSFER]  azul      — transferindo dados (piscando)
 * - [ERROR]     vermelho  — falhou (some sozinho após alguns segundos)
 */
enum class LinkPhase { OFFLINE, SEARCHING, ONLINE, TRANSFER, ERROR }

/**
 * Centraliza o "estado de conexão" que aparece na barra superior. Deriva as
 * fases a partir de sinais que já existem (painéis online, varredura em curso,
 * USB plugado) e de dois avisos pontuais de transferência/erro que as telas
 * disparam. Uma única instância vive na [br.com.painelofertas.AppContainer].
 */
class ConnectionCenter(
    private val scope: CoroutineScope,
    panels: PanelRepository,
    discovery: PanelDiscovery,
    usbController: UsbController,
) {
    private val hasNetwork = MutableStateFlow(false)
    private val wifiBusy = MutableStateFlow(false)
    private val usbBusy = MutableStateFlow(false)
    private val wifiError = MutableStateFlow(false)
    private val usbError = MutableStateFlow(false)

    init {
        // Sonda leve: o celular está em alguma rede IPv4? (para diferenciar
        // "cinza = sem rede" de "amarelo = na rede, procurando").
        scope.launch(Dispatchers.IO) {
            while (true) {
                hasNetwork.value = LocalIp.detect() != null
                delay(NETWORK_POLL_MS)
            }
        }
    }

    /** Uma tela avisa que começou a enviar/receber (bolinha azul). */
    fun transferStarted(viaUsb: Boolean) {
        if (viaUsb) { usbBusy.value = true; usbError.value = false }
        else { wifiBusy.value = true; wifiError.value = false }
    }

    /** ...e que terminou. Se falhou, pisca vermelho por alguns segundos. */
    fun transferEnded(viaUsb: Boolean, ok: Boolean) {
        if (viaUsb) { usbBusy.value = false; if (!ok) flash(usbError) }
        else { wifiBusy.value = false; if (!ok) flash(wifiError) }
    }

    private fun flash(flag: MutableStateFlow<Boolean>) {
        scope.launch { flag.value = true; delay(ERROR_FLASH_MS); flag.value = false }
    }

    private val anyOnline = panels.panels.map { list -> list.any { it.status == PanelStatus.ONLINE } }

    val wifi: StateFlow<LinkPhase> =
        combine(hasNetwork, anyOnline, discovery.scanning, wifiBusy, wifiError) { net, online, scanning, busy, error ->
            when {
                error -> LinkPhase.ERROR
                busy -> LinkPhase.TRANSFER
                online -> LinkPhase.ONLINE
                !net -> LinkPhase.OFFLINE
                else -> LinkPhase.SEARCHING // tem rede, ainda procurando (scanning=$scanning)
            }
        }.stateIn(scope, SharingStarted.Eagerly, LinkPhase.OFFLINE)

    val usb: StateFlow<LinkPhase> =
        combine(usbController.connected, usbBusy, usbError) { connected, busy, error ->
            when {
                error -> LinkPhase.ERROR
                busy -> LinkPhase.TRANSFER
                connected -> LinkPhase.ONLINE
                else -> LinkPhase.OFFLINE
            }
        }.stateIn(scope, SharingStarted.Eagerly, LinkPhase.OFFLINE)

    private companion object {
        const val NETWORK_POLL_MS = 4000L
        const val ERROR_FLASH_MS = 4000L
    }
}
