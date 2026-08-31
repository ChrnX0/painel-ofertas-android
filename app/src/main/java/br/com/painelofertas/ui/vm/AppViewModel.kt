package br.com.painelofertas.ui.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** Estado de navegação compartilhado entre as telas (escopo de Activity). */
class AppViewModel : ViewModel() {

    /**
     * Tela atual: 0=Editar, 1=Painéis, 2=Agenda, 3=Config.
     *
     * "Enviar" deixou de ser uma tela: publicar é uma ação da tela Editar, com o
     * destino escolhido na própria barra Publicar.
     */
    var selectedTab by mutableIntStateOf(0)

    companion object {
        const val TAB_EDITAR = 0
        const val TAB_PAINEIS = 1
        const val TAB_AGENDA = 2
        const val TAB_CONFIG = 3
    }
}
