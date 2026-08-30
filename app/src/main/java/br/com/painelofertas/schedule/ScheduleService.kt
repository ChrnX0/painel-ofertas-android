package br.com.painelofertas.schedule

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import br.com.painelofertas.PainelApp
import br.com.painelofertas.R
import br.com.painelofertas.data.ScheduleStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Serviço em primeiro plano que executa as tarefas agendadas — é o que faz o
 * agendamento funcionar **com o app fechado**. Sobe com uma notificação (exigência
 * do Android), varre a rede para achar o painel, envia o álbum e se encerra.
 */
class ScheduleService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification("Enviando ao painel…"))
        scope.launch {
            runCatching { executarTarefas() }
            Scheduler.reschedule(applicationContext) // agenda o próximo disparo
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private suspend fun executarTarefas() {
        val app = applicationContext as? PainelApp ?: return
        val container = app.container
        val store = ScheduleStore(applicationContext)
        val tarefas = Scheduler.dueNow(store.list())
        if (tarefas.isEmpty()) return

        // Dá um tempo para a rede subir e o painel ser localizado.
        container.autoConnect()
        withTimeoutOrNull(DESCOBERTA_MS) {
            while (container.panels.panels.value.none { it.ip.isNotBlank() }) delay(500)
        }

        for (t in tarefas) {
            val brilho = if (t.brightness in 0..100) t.brightness else null
            val ok = runCatching { container.sendAlbumByName(t.album, t.panelIp, brilho) }.getOrDefault(false)
            notify(if (ok) "✓ \"${t.album}\" enviado ao painel" else "Falha ao enviar \"${t.album}\"")
            if (!t.daily) store.remove(t.id) // tarefa de data única se consome
        }
    }

    private fun notify(texto: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm?.notify(NOTIF_ID, buildNotification(texto))
    }

    private fun buildNotification(texto: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(CHANNEL, "Agendamentos", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Envios programados para o painel de LED"
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(canal)
        }
        return NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle("Painel de Ofertas")
            .setContentText(texto)
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(false)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private companion object {
        const val CHANNEL = "agenda"
        const val NOTIF_ID = 4712
        const val DESCOBERTA_MS = 12_000L
    }
}
