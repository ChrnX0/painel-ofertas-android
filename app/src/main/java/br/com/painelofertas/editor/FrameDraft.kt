package br.com.painelofertas.editor

import br.com.painelofertas.protocol.FrameType
import br.com.painelofertas.protocol.PanelFrame
import br.com.painelofertas.protocol.PanelRecord
import br.com.painelofertas.render.AutoLayout
import br.com.painelofertas.render.FontProvider
import br.com.painelofertas.render.OfertaLayout
import br.com.painelofertas.render.OfertaSpec

/** Uma linha de texto de uma Mensagem em edição. */
data class LineDraft(
    val text: String = "TEXTO",
    val font: Int = 1,
    val row: Int = 4,
    val col: Int = 4,
)

/**
 * Um quadro editável do álbum. Preserva o estado do editor (linhas / spec da
 * oferta) para permitir reedição, e sabe se converter no [PanelFrame] final.
 */
sealed interface FrameDraft {
    val halfScreen: Boolean
    val durationIndex: Int
    /** Nome dado pelo lojista (aparece nos quadradinhos e na Sequência). Só na sessão. */
    val name: String
    fun build(fonts: FontProvider, portrait: Boolean = false): PanelFrame
    fun label(): String

    /** Rótulo a exibir: o nome, ou o tipo se não houver nome. */
    fun display(): String = name.ifBlank { label() }

    /** Mensagem de texto livre. */
    data class Msg(
        val lines: List<LineDraft> = listOf(LineDraft("BEM VINDOS", 1, 20, 6)),
        override val halfScreen: Boolean = true,
        override val durationIndex: Int = 0,
        val border: Int = 0, // 0=sem, 1=segmentada, 2=contínua
        val enabled: Boolean = true,
        override val name: String = "",
        /** Auto-justificar: o app centraliza e distribui o texto sozinho no display. */
        val autoFit: Boolean = true,
    ) : FrameDraft {
        override fun build(fonts: FontProvider, portrait: Boolean) = PanelFrame(
            type = FrameType.MENSAGEM,
            halfScreen = halfScreen,
            durationIndex = durationIndex,
            f3 = border == 2,
            f4 = border == 1,
            enabled = enabled,
            records =
                if (autoFit) {
                    AutoLayout.layout(
                        linhas = lines.map { it.text to it.font },
                        halfScreen = halfScreen,
                        portrait = portrait,
                        fonts = fonts,
                    )
                } else {
                    lines.filter { it.text.isNotBlank() }
                        .map { PanelRecord.Text(9, it.row, it.col, it.font, it.text) }
                },
        )

        override fun label() = "Mensagem"
    }

    /** Oferta de preço (spec completa). */
    data class Ofe(
        val spec: OfertaSpec = OfertaSpec(cabecalho = "OFERTA", valor = "990"),
        override val halfScreen: Boolean = true,
        override val name: String = "",
    ) : FrameDraft {
        override val durationIndex: Int get() = spec.duracaoIndex
        override fun build(fonts: FontProvider, portrait: Boolean) = OfertaLayout.build(spec, fonts, halfScreen, portrait)
        override fun label() = "Oferta"
    }

    /** Quadro carregado de um álbum salvo que não é reeditável campo-a-campo (só preview/enviar). */
    data class Raw(val frame: PanelFrame, override val name: String = "") : FrameDraft {
        override val halfScreen: Boolean get() = frame.halfScreen
        override val durationIndex: Int get() = frame.durationIndex
        override fun build(fonts: FontProvider, portrait: Boolean) = frame
        override fun label() = if (frame.type == FrameType.OFERTA) "Oferta" else "Mensagem"
    }

    companion object {
        /** Converte um quadro salvo em rascunho: Mensagens viram editáveis; o resto, Raw. */
        fun fromFrame(frame: PanelFrame): FrameDraft =
            if (frame.type == FrameType.MENSAGEM) {
                Msg(
                    lines = frame.records.filterIsInstance<PanelRecord.Text>()
                        .map { LineDraft(it.text, it.font, it.row, it.col) }
                        .ifEmpty { listOf(LineDraft()) },
                    halfScreen = frame.halfScreen,
                    durationIndex = frame.durationIndex,
                    border = if (frame.f3) 2 else if (frame.f4) 1 else 0,
                    enabled = frame.enabled,
                )
            } else {
                Raw(frame)
            }
    }
}
