package br.com.painelofertas.protocol

/** Tipo de quadro. Byte 0 = Mensagem (texto livre), 1 = Oferta (anúncio de preço). */
enum class FrameType(val code: Int) {
    MENSAGEM(0), OFERTA(1);

    companion object {
        fun of(code: Int): FrameType = if (code == 1) OFERTA else MENSAGEM
    }
}

/** Fontes bitmap `.flb` disponíveis (código 0..4). Nomes conforme a UI original. */
enum class PanelFont(val code: Int, val fileName: String, val displayName: String) {
    PRIMEIRA(0, "7x4.flb", "Primeira"),
    SEGUNDA(1, "17x8L.flb", "Segunda"),
    TERCEIRA(2, "28x16.flb", "Terceira"),
    QUARTA(3, "42x24.flb", "Quarta"),
    QUINTA(4, "60X35.flb", "Quinta");

    companion object {
        fun of(code: Int): PanelFont = entries.firstOrNull { it.code == code } ?: PRIMEIRA
    }
}

/** Um registro dentro de um quadro: texto posicionado ou uma barra/linha gráfica. */
sealed interface PanelRecord {

    /** Texto: `;LEN;SLOT;ROW;COL;FONT;TEXTO`. SLOT = campo semântico (6=Memo, 9=linha de mensagem). */
    data class Text(
        val slot: Int,
        val row: Int,
        val col: Int,
        val font: Int,
        val text: String,
    ) : PanelRecord {
        /** LEN = 4 bytes de cabeçalho + bytes do texto + CR. */
        val len: Int get() = text.length + 5

        fun toLine(): String = ";$len;$slot;$row;$col;$font;$text"
    }

    /** Gráfico (SLOT=0): `;5;0;ROW1;COL1;ROW2;COL2;` — retângulo/barra preenchida. */
    data class Graphic(
        val row1: Int,
        val col1: Int,
        val row2: Int,
        val col2: Int,
    ) : PanelRecord {
        fun toLine(): String = ";5;0;$row1;$col1;$row2;$col2;"
    }
}

/** Um quadro (Oferta ou Mensagem): cabeçalho + registros exibidos juntos. */
data class PanelFrame(
    val type: FrameType,
    val halfScreen: Boolean = true,
    val durationIndex: Int = 0,
    val f3: Boolean = false,
    val f4: Boolean = false,
    val f5: Boolean = false,
    val f6: Boolean = false,
    val enabled: Boolean = true,
    val records: List<PanelRecord> = emptyList(),
) {
    /** Serializa para linhas de bloco (`:` cabeçalho + `;` registros). */
    fun toLines(): List<String> {
        val head = buildString {
            append(':')
            append(if (type == FrameType.OFERTA) 1 else 0); append(';')
            append(if (halfScreen) 1 else 0); append(';')
            append(durationIndex); append(';')
            append(if (f3) 1 else 0); append(';')
            append(if (f4) 1 else 0); append(';')
            append(if (f5) 1 else 0); append(';')
            append(if (f6) 1 else 0); append(';')
            append(if (enabled) 1 else 0); append(';')
        }
        val body = records.map { rec ->
            when (rec) {
                is PanelRecord.Text -> rec.toLine()
                is PanelRecord.Graphic -> rec.toLine()
            }
        }
        return listOf(head) + body
    }
}

/**
 * Um álbum = conteúdo completo enviado a um painel: cabeçalho de 4 campos
 * (nome / CRC / consumo / brilho) + quadros. Corresponde a um arquivo `.alb`.
 */
data class Album(
    val name: String,
    val brilho: Int = 100,
    val frames: List<PanelFrame> = emptyList(),
    val crc: Int = 0,
    val consumo: Int = 0,
) {
    /** Linhas de bloco (sem o cabeçalho de 4 campos). */
    fun blockLines(): List<String> = frames.flatMap { it.toLines() }

    /** Texto completo do arquivo `.alb` (com CRLF, como o app Windows grava). */
    fun toAlbText(): String {
        val header = listOf(name, crc.toString(), consumo.toString(), brilho.toString())
        return (header + blockLines()).joinToString("\r\n") + "\r\n"
    }

    /** Compila este álbum para o fluxo de bytes enviado ao painel (recalcula CRC/consumo). */
    fun compile(): CompileResult {
        val lines = listOf(name, crc.toString(), consumo.toString(), brilho.toString()) + blockLines()
        return BinaryCodec.compile(lines)
    }

    companion object {
        /** Lê um arquivo `.alb` (tolerante a CRLF/LF e linhas em branco). */
        fun fromAlbText(text: String): Album {
            val lines = text.replace("\r\n", "\n").split('\n')
            val name = lines.getOrElse(0) { "Painel" }
            val crc = lines.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            val consumo = lines.getOrNull(2)?.trim()?.toIntOrNull() ?: 0
            val brilho = lines.getOrNull(3)?.trim()?.toIntOrNull() ?: 100
            val blocks = lines.drop(4).filter { it.isNotBlank() }
            return Album(name, brilho, AlbumCodec.parseFrames(blocks), crc, consumo)
        }
    }
}

/** Conversão entre linhas de bloco `:`/`;` e quadros estruturados. */
object AlbumCodec {

    /** Interpreta linhas de bloco (`:` cabeçalho + `;` registros) em quadros. */
    fun parseFrames(blockLines: List<String>): List<PanelFrame> {
        val frames = mutableListOf<PanelFrame>()
        var type = FrameType.MENSAGEM
        var half = true
        var dur = 0
        var f3 = false; var f4 = false; var f5 = false; var f6 = false
        var enabled = true
        var records = mutableListOf<PanelRecord>()
        var open = false

        fun flush() {
            if (open) frames.add(PanelFrame(type, half, dur, f3, f4, f5, f6, enabled, records))
        }

        for (raw in blockLines) {
            val linha = raw.trimEnd('\r')
            if (linha.isEmpty()) continue
            when (linha[0]) {
                ':' -> {
                    flush()
                    records = mutableListOf()
                    open = true
                    type = FrameType.of(retornaParametro(linha, 1, ':', ';', 'z').toIntOrNull() ?: 0)
                    half = retornaParametro(linha, 1, ';', ';', 'z') == "1"
                    dur = retornaParametro(linha, 2, ';', ';', 'z').toIntOrNull() ?: 0
                    f3 = retornaParametro(linha, 3, ';', ';', 'z') == "1"
                    f4 = retornaParametro(linha, 4, ';', ';', 'z') == "1"
                    f5 = retornaParametro(linha, 5, ';', ';', 'z') == "1"
                    f6 = retornaParametro(linha, 6, ';', ';', 'z') == "1"
                    enabled = retornaParametro(linha, 7, ';', ';', 'z') == "1"
                }
                ';' -> {
                    val slot = retornaParametro(linha, 2, ';', ';', 'z').toIntOrNull() ?: 0
                    if (slot == 0) {
                        records.add(
                            PanelRecord.Graphic(
                                row1 = retornaParametro(linha, 3, ';', ';', 'z').toIntOrNull() ?: 0,
                                col1 = retornaParametro(linha, 4, ';', ';', 'z').toIntOrNull() ?: 0,
                                row2 = retornaParametro(linha, 5, ';', ';', 'z').toIntOrNull() ?: 0,
                                col2 = retornaParametro(linha, 6, ';', ';', 'z').toIntOrNull() ?: 0,
                            )
                        )
                    } else {
                        records.add(
                            PanelRecord.Text(
                                slot = slot,
                                row = retornaParametro(linha, 3, ';', ';', 'z').toIntOrNull() ?: 0,
                                col = retornaParametro(linha, 4, ';', ';', 'z').toIntOrNull() ?: 0,
                                font = retornaParametro(linha, 5, ';', ';', 'z').toIntOrNull() ?: 0,
                                text = BinaryCodec.campoTexto(linha),
                            )
                        )
                    }
                }
            }
        }
        flush()
        return frames
    }
}
