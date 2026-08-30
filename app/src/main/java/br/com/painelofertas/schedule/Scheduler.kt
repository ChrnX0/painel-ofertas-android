package br.com.painelofertas.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import br.com.painelofertas.data.ScheduledTask
import br.com.painelofertas.data.ScheduleStore
import java.util.Calendar

/**
 * Agendamento **em segundo plano**: o envio acontece na hora marcada mesmo com o
 * app fechado. Usa `AlarmManager` (nativo, sem dependência nova e mais pontual
 * que um job periódico) para acordar o [ScheduleReceiver], que sobe o
 * [ScheduleService] para fazer a transferência.
 *
 * Estratégia: agenda sempre **o próximo disparo**; ao executar, reagenda o
 * seguinte. Assim uma tarefa diária se auto-renova e não acumula alarmes.
 */
object Scheduler {

    /** (Re)agenda o alarme para a tarefa mais próxima. Chame após mexer na agenda. */
    fun reschedule(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val store = ScheduleStore(context)
        val proxima = store.list().mapNotNull { t -> nextTrigger(t)?.let { t to it } }.minByOrNull { it.second }

        val pi = alarmIntent(context)
        am.cancel(pi)
        if (proxima == null) return

        val quando = proxima.second
        // setAndAllowWhileIdle: dispara mesmo em Doze, sem exigir a permissão de
        // alarme exato (que o Android 12+ pede ao usuário). Tolerância de minutos
        // é aceitável para trocar a oferta da manhã/tarde.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando, pi)
        } else {
            am.set(AlarmManager.RTC_WAKEUP, quando, pi)
        }
    }

    /** Instante (epoch ms) do próximo disparo desta tarefa, ou null se já passou. */
    fun nextTrigger(t: ScheduledTask, now: Long = System.currentTimeMillis()): Long? {
        val c = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, t.hour)
            set(Calendar.MINUTE, t.minute)
        }
        if (t.daily) {
            if (c.timeInMillis <= now) c.add(Calendar.DAY_OF_MONTH, 1) // hoje já passou → amanhã
            return c.timeInMillis
        }
        c.set(Calendar.YEAR, t.year)
        c.set(Calendar.MONTH, t.month - 1)
        c.set(Calendar.DAY_OF_MONTH, t.day)
        return if (c.timeInMillis > now) c.timeInMillis else null
    }

    /** Tarefas que devem disparar agora (janela de tolerância de alguns minutos). */
    fun dueNow(tasks: List<ScheduledTask>, now: Long = System.currentTimeMillis()): List<ScheduledTask> {
        val c = Calendar.getInstance().apply { timeInMillis = now }
        val h = c.get(Calendar.HOUR_OF_DAY)
        val mi = c.get(Calendar.MINUTE)
        val d = c.get(Calendar.DAY_OF_MONTH)
        val mo = c.get(Calendar.MONTH) + 1
        val y = c.get(Calendar.YEAR)
        return tasks.filter { t ->
            val mesmaHora = t.hour == h && (mi - t.minute) in 0..TOLERANCIA_MIN
            mesmaHora && (t.daily || (t.day == d && t.month == mo && t.year == y))
        }
    }

    private fun alarmIntent(context: Context): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getBroadcast(
            context, REQ_CODE,
            Intent(context, ScheduleReceiver::class.java).setAction(ACTION_FIRE),
            flags,
        )
    }

    const val ACTION_FIRE = "br.com.painelofertas.AGENDA_DISPARO"
    private const val REQ_CODE = 4711
    private const val TOLERANCIA_MIN = 5
}

/**
 * Acordado pelo alarme (ou pelo boot). Sobe o serviço que faz o envio e reagenda
 * o próximo disparo.
 */
class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Scheduler.reschedule(context) // o celular reiniciou: recria os alarmes
            return
        }
        ContextCompat.startForegroundService(context, Intent(context, ScheduleService::class.java))
    }
}
