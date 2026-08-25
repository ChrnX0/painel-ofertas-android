package br.com.painelofertas.protocol

import org.junit.Assert.assertEquals
import org.junit.Test

class ProtocolFieldsTest {

    @Test
    fun retorna_parametro_header_fields() {
        val h = ":0;1;0;0;1;0;0;1;"
        assertEquals("0", retornaParametro(h, 1, ':', ';', 'z')) // TYPE
        assertEquals("1", retornaParametro(h, 1, ';', ';', 'z')) // ADSIZE
        assertEquals("0", retornaParametro(h, 2, ';', ';', 'z')) // DUR
        assertEquals("1", retornaParametro(h, 4, ';', ';', 'z')) // F4
        assertEquals("1", retornaParametro(h, 7, ';', ';', 'z')) // ENABLE
    }

    @Test
    fun retorna_parametro_text_fields() {
        val t = ";15;9;68;7;1;* VINDOS *"
        assertEquals("15", retornaParametro(t, 1, ';', ';', 'z')) // LEN
        assertEquals("9", retornaParametro(t, 2, ';', ';', 'z'))  // SLOT
        assertEquals("68", retornaParametro(t, 3, ';', ';', 'z')) // ROW
        assertEquals("7", retornaParametro(t, 4, ';', ';', 'z'))  // COL
        assertEquals("1", retornaParametro(t, 5, ';', ';', 'z'))  // FONT
    }

    @Test
    fun campo_texto_keeps_full_text_including_spaces() {
        assertEquals("* VINDOS *", BinaryCodec.campoTexto(";15;9;68;7;1;* VINDOS *"))
        assertEquals(",", BinaryCodec.campoTexto(";6;4;35;65;2;,"))
    }

    @Test
    fun duration_table_roundtrip() {
        for (i in 0..16) {
            val b = DurationTable.idxToByte(i.toString())
            assertEquals(i.toString(), DurationTable.byteToIdx(b))
        }
        assertEquals(15, DurationTable.idxToByte("10"))
        assertEquals(60, DurationTable.idxToByte("16"))
    }
}
