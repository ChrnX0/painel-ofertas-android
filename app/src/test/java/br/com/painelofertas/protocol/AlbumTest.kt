package br.com.painelofertas.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumTest {

    private val mensagemBlocks = listOf(
        ":0;1;0;0;1;0;0;1;",
        ";15;9;68;7;1;* VINDOS *",
        ";8;9;32;21;2;BEM",
        ";14;9;7;9;1;* SEJAM *",
        ":0;1;0;0;0;0;0;1;",
        ";10;9;17;22;1;FELIZ",
        ";10;9;48;1;2;NATAL",
    )

    private val nelPaiBlocks = listOf(
        ":1;1;0;0;0;0;1;1;",
        ";7;4;42;8;3;12",
        ";6;4;35;65;2;,",
        ";7;4;42;72;1;34",
        ";5;0;61;72;61;89;",
        ";10;1;5;37;0;TESTE",
        ";10;2;16;23;1;TESTE",
    )

    @Test
    fun frames_roundtrip_mensagem() {
        val frames = AlbumCodec.parseFrames(mensagemBlocks)
        assertEquals(2, frames.size)
        assertEquals(FrameType.MENSAGEM, frames[0].type)
        assertEquals(mensagemBlocks, frames.flatMap { it.toLines() })
    }

    @Test
    fun frames_roundtrip_nelPai_with_graphic() {
        val frames = AlbumCodec.parseFrames(nelPaiBlocks)
        assertEquals(1, frames.size)
        assertEquals(FrameType.OFERTA, frames[0].type)
        // deve conter a barra gráfica (SLOT=0)
        assertTrue(frames[0].records.any { it is PanelRecord.Graphic })
        assertEquals(nelPaiBlocks, frames.flatMap { it.toLines() })
    }

    @Test
    fun text_record_len_is_derived() {
        val rec = PanelRecord.Text(slot = 9, row = 1, col = 2, font = 1, text = "ABERTOS")
        assertEquals(12, rec.len) // 7 + 5
        assertEquals(";12;9;1;2;1;ABERTOS", rec.toLine())
    }

    @Test
    fun album_alb_text_roundtrip() {
        val album = Album(
            name = "Painel 1",
            brilho = 50,
            frames = AlbumCodec.parseFrames(nelPaiBlocks),
        )
        val parsed = Album.fromAlbText(album.toAlbText())
        assertEquals("Painel 1", parsed.name)
        assertEquals(50, parsed.brilho)
        assertEquals(nelPaiBlocks, parsed.blockLines())
    }

    @Test
    fun album_compile_matches_line_based_compile() {
        val album = Album(name = "Painel 1", brilho = 100, frames = AlbumCodec.parseFrames(nelPaiBlocks))
        val viaAlbum = album.compile()
        val viaLines = BinaryCodec.compile(listOf("Painel 1", "0", "0", "100") + nelPaiBlocks)
        assertEquals(viaLines.crc, viaAlbum.crc)
        assertEquals(viaLines.consumo, viaAlbum.consumo)
    }
}
