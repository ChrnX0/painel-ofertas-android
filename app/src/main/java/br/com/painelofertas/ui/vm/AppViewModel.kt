package br.com.painelofertas.ui.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** Estado de navegação compartilhado entre as abas (escopo de Activity). */
class AppViewModel : ViewModel() {

    /** Aba atual: 0=Editar, 1=Enviar, 2=Painéis, 3=Agenda, 4=Config. */
    var selectedTab by mutableIntStateOf(0)

    /** Álbum a pré-selecionar ao abrir a aba Enviar (fluxo "Salvar e enviar"). */
    var pendingSendAlbum by mutableStateOf<String?>(null)
        private set

    /** Vai para a aba Enviar com [album] já selecionado. */
    fun goToSend(album: String) {
        pendingSendAlbum = album
        selectedTab = TAB_ENVIAR
    }

    fun consumePendingSend(): String? {
        val a = pendingSendAlbum
        pendingSendAlbum = null
        return a
    }

    companion object {
        const val TAB_EDITAR = 0
        const val TAB_ENVIAR = 1
        const val TAB_PAINEIS = 2
        const val TAB_AGENDA = 3
        const val TAB_CONFIG = 4
    }
}
