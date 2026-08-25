package br.com.painelofertas.usb

import android.app.PendingIntent
import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager

/**
 * Localiza e abre o controlador do painel via USB HID (ponte Microchip
 * VID 0x04D8 / PID 0xF002). Envio/recepção usam o endpoint de interrupção.
 *
 * NOTA: o report do original tem 65 bytes com o byte 0 = ReportID (convenção do
 * Windows HID). No Android, via endpoint de interrupção, transmitimos apenas o
 * corpo de 64 bytes (o ReportID não vai no fio para reports não-numerados).
 * Precisa de validação contra o dispositivo real.
 */
class UsbHidManager(context: Context) {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    fun findDevice(): UsbDevice? =
        usbManager.deviceList.values.firstOrNull { it.vendorId == VID && it.productId == PID }

    fun hasPermission(device: UsbDevice): Boolean = usbManager.hasPermission(device)

    fun requestPermission(device: UsbDevice, intent: PendingIntent) =
        usbManager.requestPermission(device, intent)

    /** Abre a conexão, reivindica a interface HID e localiza os endpoints. */
    fun open(device: UsbDevice): UsbHidConnection? {
        val connection = usbManager.openDevice(device) ?: return null
        // Interface HID (classe 3); cai para a interface 0 se não achar.
        var hid: UsbInterface? = null
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_HID) { hid = iface; break }
        }
        val iface = hid ?: device.getInterface(0)
        if (!connection.claimInterface(iface, true)) { connection.close(); return null }

        var epIn: UsbEndpoint? = null
        var epOut: UsbEndpoint? = null
        for (e in 0 until iface.endpointCount) {
            val ep = iface.getEndpoint(e)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_INT) {
                if (ep.direction == UsbConstants.USB_DIR_IN) epIn = ep else epOut = ep
            }
        }
        if (epOut == null) { connection.releaseInterface(iface); connection.close(); return null }
        return UsbHidConnection(connection, iface, epOut, epIn)
    }

    companion object {
        const val VID = 1240      // 0x04D8 Microchip
        const val PID = 61442     // 0xF002
        const val REPORT_SIZE = 64
    }
}

/** Conexão HID aberta: envio/recepção de reports de 64 bytes. */
class UsbHidConnection(
    private val connection: UsbDeviceConnection,
    private val iface: UsbInterface,
    private val epOut: UsbEndpoint,
    private val epIn: UsbEndpoint?,
) {
    /** Envia um corpo de report (até 64 bytes) pelo endpoint de interrupção OUT. */
    fun send(body: ByteArray, timeoutMs: Int = 200): Boolean {
        val out = if (body.size == UsbHidManager.REPORT_SIZE) body
        else body.copyOf(UsbHidManager.REPORT_SIZE)
        return connection.bulkTransfer(epOut, out, out.size, timeoutMs) >= 0
    }

    /** Lê um report do endpoint IN (null se nada chegou dentro do timeout). */
    fun read(timeoutMs: Int = 200): ByteArray? {
        val ep = epIn ?: return null
        val buf = ByteArray(UsbHidManager.REPORT_SIZE)
        val n = connection.bulkTransfer(ep, buf, buf.size, timeoutMs)
        return if (n >= 0) buf.copyOf(n.coerceAtLeast(0)) else null
    }

    fun close() {
        runCatching { connection.releaseInterface(iface) }
        runCatching { connection.close() }
    }
}
