package br.com.painelofertas.data

import android.content.Context
import br.com.painelofertas.protocol.Album
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Salva/carrega álbuns como arquivos `.alb`. Expõe a lista de nomes como
 * [StateFlow] observável, então salvar/excluir atualiza todas as telas sozinho
 * (sem botão "Atualizar lista").
 */
class AlbumStore(context: Context) {

    private val dir = File(context.filesDir, "albums").apply { mkdirs() }

    private val _names = MutableStateFlow(scan())
    val names: StateFlow<List<String>> = _names.asStateFlow()

    private fun scan(): List<String> =
        dir.listFiles { f -> f.isFile && f.name.endsWith(".alb") }
            ?.map { it.name.removeSuffix(".alb") }
            ?.sorted()
            ?: emptyList()

    fun list(): List<String> = _names.value

    fun save(album: Album) {
        File(dir, safe(album.name) + ".alb").writeText(album.toAlbText(), Charsets.ISO_8859_1)
        _names.value = scan()
    }

    fun load(name: String): Album? {
        val f = File(dir, safe(name) + ".alb")
        return if (f.exists()) Album.fromAlbText(f.readText(Charsets.ISO_8859_1)) else null
    }

    fun delete(name: String) {
        File(dir, safe(name) + ".alb").delete()
        _names.value = scan()
    }

    private fun safe(name: String) = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifEmpty { "Painel" }
}

/** Configurações persistentes (era Advanced.dll/config.ini). */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("painel_ofertas", Context.MODE_PRIVATE)

    var localIp: String
        get() = prefs.getString("localIp", "") ?: ""
        set(v) = prefs.edit().putString("localIp", v).apply()

    /** Efeito global de tela: 0=Padrão, 1=Pisca/Inverte, 2=Pisca/Padrão. */
    var effectMode: Int
        get() = prefs.getInt("effectMode", 0)
        set(v) = prefs.edit().putInt("effectMode", v).apply()

    var dhcp: Boolean
        get() = prefs.getBoolean("dhcp", true)
        set(v) = prefs.edit().putBoolean("dhcp", v).apply()

    var useTxPassword: Boolean
        get() = prefs.getBoolean("useTxPassword", false)
        set(v) = prefs.edit().putBoolean("useTxPassword", v).apply()

    /** Senha de transmissão digitada localmente (para desbloquear o envio). */
    var txPassword: String
        get() = prefs.getString("txPassword", "") ?: ""
        set(v) = prefs.edit().putString("txPassword", v).apply()

    /** Tema: 0 = sistema, 1 = claro, 2 = escuro. Observável para trocar na hora. */
    private val _themeMode = MutableStateFlow(prefs.getInt("themeMode", 0))
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    fun setThemeMode(value: Int) {
        prefs.edit().putInt("themeMode", value).apply()
        _themeMode.value = value
    }

    /** Cor do LED na prévia (índice em LedColors): 0=âmbar,1=vermelho,2=verde,3=azul,4=branco. */
    private val _ledColor = MutableStateFlow(prefs.getInt("ledColor", 0))
    val ledColor: StateFlow<Int> = _ledColor.asStateFlow()

    fun setLedColor(value: Int) {
        prefs.edit().putInt("ledColor", value).apply()
        _ledColor.value = value
    }
}
