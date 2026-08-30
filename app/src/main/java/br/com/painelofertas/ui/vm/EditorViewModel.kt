package br.com.painelofertas.ui.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import br.com.painelofertas.editor.FrameDraft
import br.com.painelofertas.protocol.Album

/**
 * Estado do editor de álbum. Vive num [ViewModel] com escopo de Activity, então
 * sobrevive à troca de aba e à rotação da tela (o lojista não perde o trabalho).
 */
class EditorViewModel : ViewModel() {

    var nome by mutableStateOf("Álbum 1")
    val frames: SnapshotStateList<FrameDraft> = mutableStateListOf(FrameDraft.Ofe())
    var selected by mutableIntStateOf(0)

    /** Orientação do painel: false = horizontal (deitado), true = vertical (em pé). */
    var portrait by mutableStateOf(false)

    /** Snapshot do que está gravado no painel (para a prévia "ao vivo" deslizável). */
    var liveAlbum by mutableStateOf<Album?>(null)
        private set

    /** CRC do painel no último sync — re-sincroniza sozinho só quando muda. */
    var liveCrc by mutableIntStateOf(0)
        private set

    /** Painel (id) do último sync, para saber se trocou de painel-alvo. */
    var livePanelId by mutableStateOf("")
        private set

    fun setLive(album: Album?, crc: Int = 0, panelId: String = "") {
        liveAlbum = album; liveCrc = crc; livePanelId = panelId
    }

    val sel: Int get() = selected.coerceIn(0, (frames.size - 1).coerceAtLeast(0))
    val current: FrameDraft? get() = frames.getOrNull(sel)

    fun replaceSel(d: FrameDraft) { if (sel in frames.indices) frames[sel] = d }

    /**
     * Alterna o modo da tela atual: **Padrão** (Oferta estruturada) ou **Livre**
     * (Mensagem — texto solto com posição/fonte). Converte o tipo do quadro,
     * preservando o nome (o conteúdo é reiniciado, pois os modelos são diferentes).
     */
    fun setMode(livre: Boolean) {
        val d = current ?: return
        if (livre && d !is FrameDraft.Msg) {
            frames[sel] = FrameDraft.Msg(name = d.name, halfScreen = d.halfScreen, durationIndex = d.durationIndex)
        } else if (!livre && d !is FrameDraft.Ofe) {
            frames[sel] = FrameDraft.Ofe(name = d.name, halfScreen = d.halfScreen)
        }
    }

    fun addMsg(name: String = "") { frames.add(FrameDraft.Msg(name = name)); selected = frames.lastIndex }
    fun addOfe(name: String = "") { frames.add(FrameDraft.Ofe(name = name)); selected = frames.lastIndex }

    /** Renomeia a tela [index] (o nome aparece nos quadradinhos e na Sequência). */
    fun renameFrame(index: Int, newName: String) {
        if (index in frames.indices) {
            frames[index] = when (val d = frames[index]) {
                is FrameDraft.Msg -> d.copy(name = newName)
                is FrameDraft.Ofe -> d.copy(name = newName)
                is FrameDraft.Raw -> d.copy(name = newName)
            }
        }
    }

    fun duplicate() {
        if (sel in frames.indices) { frames.add(sel + 1, frames[sel]); selected = sel + 1 }
    }

    /** Último quadro excluído (índice + rascunho), para permitir "Desfazer". */
    var lastDeleted: Pair<Int, FrameDraft>? = null
        private set

    fun delete() {
        if (frames.size > 1 && sel in frames.indices) {
            lastDeleted = sel to frames[sel]
            frames.removeAt(sel); selected = (sel - 1).coerceAtLeast(0)
        }
    }

    fun undoDelete() {
        val (idx, frame) = lastDeleted ?: return
        val at = idx.coerceIn(0, frames.size)
        frames.add(at, frame)
        selected = at
        lastDeleted = null
    }

    fun moveUp() {
        if (sel > 0) { val t = frames[sel]; frames[sel] = frames[sel - 1]; frames[sel - 1] = t; selected = sel - 1 }
    }

    fun moveDown() {
        if (sel < frames.lastIndex) { val t = frames[sel]; frames[sel] = frames[sel + 1]; frames[sel + 1] = t; selected = sel + 1 }
    }

    fun setDuration(idx: Int) {
        when (val d = current) {
            is FrameDraft.Msg -> replaceSel(d.copy(durationIndex = idx))
            is FrameDraft.Ofe -> replaceSel(d.copy(spec = d.spec.copy(duracaoIndex = idx)))
            else -> {}
        }
    }

    fun load(album: Album) {
        nome = album.name
        frames.clear()
        album.frames.forEach { frames.add(FrameDraft.fromFrame(it)) }
        if (frames.isEmpty()) frames.add(FrameDraft.Msg())
        selected = 0
    }

    /** Começa um álbum novo em branco (uma Oferta), zerando a prévia ao vivo. */
    fun newAlbum() {
        nome = "Álbum 1"
        frames.clear()
        frames.add(FrameDraft.Ofe())
        selected = 0
        liveAlbum = null
    }

    /**
     * Sincronizar: coloca as telas lidas do painel ANTES das que o usuário está
     * montando, para ele ver a sequência real e reordenar/inserir a sua no lugar
     * certo. Seleciona a 1ª tela do usuário (logo após as do painel).
     */
    fun mergePanelFrames(album: Album) {
        if (album.frames.isEmpty()) return
        val minhas = frames.toList()
        frames.clear()
        album.frames.forEach { frames.add(FrameDraft.fromFrame(it)) }
        val firstMine = frames.size
        minhas.forEach { frames.add(it) }
        if (frames.isEmpty()) frames.add(FrameDraft.Msg())
        selected = firstMine.coerceIn(0, frames.lastIndex)
    }

    /** Álbum atual (para salvar/enviar), construído a partir dos rascunhos. */
    fun toAlbum(fonts: br.com.painelofertas.render.FontProvider): Album =
        Album(name = nome, frames = frames.map { it.build(fonts, portrait) })
}
