package br.com.painelofertas.net

import java.net.Inet4Address
import java.net.NetworkInterface

/** Descobre o IPv4 local do aparelho (para anunciar o servidor e configurar o módulo). */
object LocalIp {
    fun detect(): String? {
        return runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                // prefere a interface Wi-Fi (wlan*) — é a rede em que o painel está;
                // evita pegar o IP de dados móveis/VPN quando ambos estão ativos.
                .sortedByDescending { it.name.startsWith("wlan") }
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it is Inet4Address && it.isSiteLocalAddress }
                ?.hostAddress
        }.getOrNull()
    }
}
