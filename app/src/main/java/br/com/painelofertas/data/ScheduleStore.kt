package br.com.painelofertas.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Uma tarefa agendada: envia [album] para [panelIp] na hora indicada.
 * Se [daily] = true, dispara todo dia nesse horário (ignora a data); senão, uma
 * única vez na data. [brightness] em 0..100 sobrescreve o brilho do álbum (-1 = usa o do álbum).
 */
data class ScheduledTask(
    val id: String,
    val hour: Int,
    val minute: Int,
    val day: Int,
    val month: Int,
    val year: Int,
    val album: String,
    val panelIp: String,
    val daily: Boolean = false,
    val brightness: Int = -1,
) {
    fun dueAt(h: Int, mi: Int, d: Int, mo: Int, y: Int): Boolean =
        hour == h && minute == mi && (daily || (day == d && month == mo && year == y))
}

/** Persistência das tarefas de agenda (era agentask.dll). */
class ScheduleStore(context: Context) {

    private val prefs = context.getSharedPreferences("painel_agenda", Context.MODE_PRIVATE)

    fun list(): List<ScheduledTask> {
        val arr = JSONArray(prefs.getString("tasks", "[]"))
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ScheduledTask(
                id = o.getString("id"),
                hour = o.getInt("hour"), minute = o.getInt("minute"),
                day = o.getInt("day"), month = o.getInt("month"), year = o.getInt("year"),
                album = o.getString("album"), panelIp = o.getString("panelIp"),
                daily = o.optBoolean("daily", false), brightness = o.optInt("brightness", -1),
            )
        }
    }

    fun add(task: ScheduledTask) = save(list() + task)
    fun remove(id: String) = save(list().filterNot { it.id == id })

    private fun save(tasks: List<ScheduledTask>) {
        val arr = JSONArray()
        tasks.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id); put("hour", t.hour); put("minute", t.minute)
                put("day", t.day); put("month", t.month); put("year", t.year)
                put("album", t.album); put("panelIp", t.panelIp)
                put("daily", t.daily); put("brightness", t.brightness)
            })
        }
        prefs.edit().putString("tasks", arr.toString()).apply()
    }
}
