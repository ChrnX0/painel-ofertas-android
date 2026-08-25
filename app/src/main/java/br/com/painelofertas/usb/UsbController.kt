package br.com.painelofertas.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Identificação do dispositivo USB conectado (descritores lidos com segurança). */
data class UsbInfo(
    val vendorId: Int,
    val productId: Int,
    val manufacturer: String?,
    val product: String?,
    val serial: String?,
)

/**
 * Gerencia a conexão USB com o painel: detecta o dispositivo (VID/PID), pede
 * permissão ao usuário, abre a conexão e expõe um [UsbLink] observável.
 * Porte de HIDCtrlEnumerate/HIDCtrlDeviceChange (Ofertas.pas).
 */
class UsbController(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val manager = UsbHidManager(context)

    private val _link = MutableStateFlow<UsbLink?>(null)
    val link: StateFlow<UsbLink?> = _link

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    private val _info = MutableStateFlow<UsbInfo?>(null)
    val info: StateFlow<UsbInfo?> = _info

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                ACTION_PERMISSION -> {
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    val device = usbDevice(intent)
                    if (granted && device != null) open(device)
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> ensureConnected()
                UsbManager.ACTION_USB_DEVICE_DETACHED -> disconnect()
            }
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(ACTION_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        ensureConnected()
    }

    /** Se o painel está plugado, abre (ou pede permissão). */
    fun ensureConnected() {
        val device = manager.findDevice() ?: return
        if (manager.hasPermission(device)) open(device) else requestPermission(device)
    }

    private fun requestPermission(device: UsbDevice) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pi = PendingIntent.getBroadcast(context, 0, Intent(ACTION_PERMISSION).setPackage(context.packageName), flags)
        manager.requestPermission(device, pi)
    }

    private fun open(device: UsbDevice) {
        val conn = manager.open(device) ?: return
        _link.value?.close()
        _link.value = UsbLink(conn, scope)
        _connected.value = true
        _info.value = UsbInfo(
            vendorId = device.vendorId,
            productId = device.productId,
            manufacturer = runCatching { device.manufacturerName }.getOrNull(),
            product = runCatching { device.productName }.getOrNull(),
            serial = runCatching { device.serialNumber }.getOrNull(),
        )
    }

    fun disconnect() {
        _link.value?.close()
        _link.value = null
        _connected.value = false
        _info.value = null
    }

    @Suppress("DEPRECATION")
    private fun usbDevice(intent: Intent): UsbDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        else intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

    companion object {
        const val ACTION_PERMISSION = "br.com.painelofertas.USB_PERMISSION"
    }
}
