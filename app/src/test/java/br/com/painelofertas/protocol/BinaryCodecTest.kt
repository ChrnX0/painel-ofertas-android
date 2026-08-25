package br.com.painelofertas.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes de fidelidade do codec binário contra os arquivos REAIS do app Windows.
 *
 * Os "vetores-ouro" (bytes esperados e CRC) foram extraídos de mensagem.dll e
 * nelPai.dll e validados por uma implementação de referência em Python. Se estes
 * testes passam, o codec Kotlin gera exatamente os mesmos bytes que o app original.
 */
class BinaryCodecTest {

    private val ALB_HEADER = listOf("Painel 1", "0", "0", "100")

    // ---- nelPai.dll (uma Oferta de preço com barra de sublinhado) ----
    private val nelPaiBlocks = listOf(
        ":1;1;0;0;0;0;1;1;",
        ";7;4;42;8;3;12",
        ";6;4;35;65;2;,",
        ";7;4;42;72;1;34",
        ";5;0;61;72;61;89;",
        ";10;1;5;37;0;TESTE",
        ";10;2;16;23;1;TESTE",
    )
    private val nelPaiBytesHex =
        "63 00 04 2A 08 03 31 32 0D 04 23 41 02 2C 0D 04 2A 48 01 33 34 0D " +
        "00 3D 48 3D 59 01 05 25 00 54 45 53 54 45 0D 02 10 17 01 54 45 53 54 45 0D FF FF"
    private val nelPaiCrc = 0xD644

    // ---- mensagem.dll (3 Mensagens de texto) ----
    private val mensagemBlocks = listOf(
        ":0;1;0;0;1;0;0;1;",
        ";15;9;68;7;1;* VINDOS *",
        ";8;9;32;21;2;BEM",
        ";14;9;7;9;1;* SEJAM *",
        ":0;1;0;0;0;0;0;1;",
        ";10;9;17;22;1;FELIZ",
        ";10;9;48;1;2;NATAL",
        ":0;1;0;0;1;0;0;1;",
        ";8;9;28;36;2;9HS",
        ";8;9;38;12;0;DAS",
        ";7;9;72;4;0;AS",
        ";9;9;62;18;2;20HS",
        ";12;9;4;15;1;ABERTOS",
    )
    private val mensagemBytesHex =
        "29 00 09 44 07 01 2A 20 56 49 4E 44 4F 53 20 2A 0D 09 20 15 02 42 45 4D 0D " +
        "09 07 09 01 2A 20 53 45 4A 41 4D 20 2A 0D FF 21 00 09 11 16 01 46 45 4C 49 5A 0D " +
        "09 30 01 02 4E 41 54 41 4C 0D FF 29 00 09 1C 24 02 39 48 53 0D 09 26 0C 00 44 41 53 0D " +
        "09 48 04 00 41 53 0D 09 3E 12 02 32 30 48 53 0D 09 04 0F 01 41 42 45 52 54 4F 53 0D FF FF"
    private val mensagemCrc = 0x06A2

    private fun hex(s: String): IntArray =
        s.trim().split(Regex("\\s+")).map { it.toInt(16) }.toIntArray()

    @Test
    fun crc_xmodem_canonical_vector() {
        // "123456789" -> 0x31C3 (vetor canônico do CRC-16/XMODEM), + 2 bytes ignorados no fim
        val data = "123456789".map { it.code }.toIntArray() + intArrayOf(0, 0)
        assertEquals(0x31C3, Crc16.xmodem(data))
    }

    @Test
    fun compile_nelPai_matches_golden_bytes_and_crc() {
        val r = BinaryCodec.compile(ALB_HEADER + nelPaiBlocks)
        assertArrayEquals(hex(nelPaiBytesHex), r.bytes)
        assertEquals(49, r.consumo)
        assertEquals(nelPaiCrc, r.crc)
        assertEquals(100, r.brilho)
    }

    @Test
    fun compile_mensagem_matches_golden_bytes_and_crc() {
        val r = BinaryCodec.compile(ALB_HEADER + mensagemBlocks)
        assertArrayEquals(hex(mensagemBytesHex), r.bytes)
        assertEquals(111, r.consumo)
        assertEquals(mensagemCrc, r.crc)
    }

    @Test
    fun decompile_is_inverse_of_compile_nelPai() {
        val bytes = BinaryCodec.compile(ALB_HEADER + nelPaiBlocks).bytes
        assertEquals(nelPaiBlocks, BinaryCodec.decompile(bytes))
    }

    @Test
    fun decompile_is_inverse_of_compile_mensagem() {
        val bytes = BinaryCodec.compile(ALB_HEADER + mensagemBlocks).bytes
        assertEquals(mensagemBlocks, BinaryCodec.decompile(bytes))
    }

    @Test
    fun text_is_sanitized_no_structural_bytes_and_accents_mapped() {
        // Texto com acentos e um caractere fora da faixa (ÿ). Nenhum byte de conteúdo
        // pode virar 0xFF (separador de bloco), e os acentos viram placeholders a..l.
        val blocks = listOf(":0;1;0;0;0;0;0;1;", ";10;9;5;5;1;AÇÃOÿ")
        val r = BinaryCodec.compile(ALB_HEADER + blocks)
        val content = r.bytes.toList().dropLast(2) // remove o FF FF final (fim de fluxo)
        assertTrue("nenhum 0xFF no conteúdo", content.none { it == 255 })
        val txt = BinaryCodec.decompile(r.bytes).last()
        assertTrue("Ç->i, Ã->b (placeholders)", txt.contains("AibO"))
    }
}
