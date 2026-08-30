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
import br.com.painelofertas.render.AutoLayout

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

    /** O rascunho automático já foi restaurado nesta sessão? (evita repetir). */
    var draftRestored by mutableStateOf(false)

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

    // ===== Publicação (salvar + enviar numa ação só) =====

    /** Fase da publicação, para a barra fixa do editor dar retorno claro. */
    enum class PubState { IDLE, WORKING, DONE, ERROR }

    var pubState by mutableStateOf(PubState.IDLE)
        private set
    var pubProgress by mutableStateOf<Float?>(null)
        private set
    var pubMessage by mutableStateOf("")
        private set

    /** Marca o resultado da publicação (a UI mostra e some sozinho depois). */
    fun pubStart() { pubState = PubState.WORKING; pubProgress = 0f; pubMessage = "Preparando…" }
    fun pubProgress(sent: Int, total: Int) {
        pubProgress = if (total > 0) sent.toFloat() / total else null
        pubMessage = "Enviando… ${(100f * sent / total.coerceAtLeast(1)).toInt()}%"
    }
    fun pubDone(msg: String) { pubState = PubState.DONE; pubProgress = 1f; pubMessage = msg }
    fun pubError(msg: String) { pubState = PubState.ERROR; pubProgress = null; pubMessage = msg }
    fun pubReset() { pubState = PubState.IDLE; pubProgress = null; pubMessage = "" }

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

    /**
     * **Criação em lote**: cada linha da lista vira uma tela pronta, com a
     * composição inteligente (preço em destaque) e nome tirado do produto.
     *
     * `PICANHA 9,90 O KILO` → tela "Picanha". N ofertas pelo custo de digitar uma
     * lista — é o dia a dia de quem troca 20 preços por semana.
     */
    fun addBatch(texto: String, substituir: Boolean): Int {
        val linhas = texto.split('\n').map { it.trim() }.filter { it.isNotBlank() }
        if (linhas.isEmpty()) return 0
        if (substituir) frames.clear()
        linhas.forEach { linha ->
            frames.add(FrameDraft.Msg(name = nomeDaOferta(linha), freeText = linha, autoFit = true, smart = true))
        }
        selected = frames.lastIndex.coerceAtLeast(0)
        return linhas.size
    }

    /** Nome curto da tela a partir da linha: o que vem antes do preço (ou as 2 primeiras palavras). */
    private fun nomeDaOferta(linha: String): String {
        val antes = AutoLayout.smartSplit(linha).firstOrNull()?.takeIf { !it.hero }?.text
        val base = antes ?: linha
        return base.split(Regex("\\s+")).take(2).joinToString(" ")
            .lowercase().replaceFirstChar { it.uppercase() }
    }

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

    /** Restaura o rascunho salvo (nome, orientação e telas já editáveis). */
    fun restoreDraft(d: Triple<String, Boolean, List<FrameDraft>>) {
        nome = d.first
        portrait = d.second
        frames.clear()
        frames.addAll(d.third)
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
