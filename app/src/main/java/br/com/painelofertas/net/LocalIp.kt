package br.com.painelofertas.net

import java.net.Inet4Address
import java.net.NetworkInterface

/** Descobre o IPv4 local do aparelho (para anunciar o servidor e configurar o módulo). */
object LocalIp {
    fun detect(): String? {
        return runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it is Inet4Address && it.isSiteLocalAddress }
                ?.hostAddress
        }.getOrNull()
    }
}
