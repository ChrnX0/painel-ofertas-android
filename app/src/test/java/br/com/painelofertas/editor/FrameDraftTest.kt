package br.com.painelofertas.editor

import br.com.painelofertas.protocol.FrameType
import br.com.painelofertas.render.FlbFont
import br.com.painelofertas.render.FontProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameDraftTest {

    private val fonts = FontProvider {
        FlbFont.fromText(javaClass.getResourceAsStream("/7x4.flb")!!.readBytes().toString(Charsets.ISO_8859_1))
    }

    @Test
    fun msg_builds_and_reverses() {
        val msg = FrameDraft.Msg(
            lines = listOf(LineDraft("BEM", 1, 20, 6), LineDraft("VINDOS", 2, 40, 8)),
            halfScreen = true, durationIndex = 5, border = 1,
        )
        val frame = msg.build(fonts)
        assertEquals(FrameType.MENSAGEM, frame.type)
        assertEquals(2, frame.records.size)

        val back = FrameDraft.fromFrame(frame)
        assertTrue(back is FrameDraft.Msg)
        back as FrameDraft.Msg
        assertEquals(2, back.lines.size)
        assertEquals("BEM", back.lines[0].text)
        assertEquals(1, back.border) // segmentada
        assertEquals(5, back.durationIndex)
    }

    @Test
    fun blank_lines_are_dropped_on_build() {
        val msg = FrameDraft.Msg(lines = listOf(LineDraft("OI", 1, 1, 1), LineDraft("", 1, 2, 2)))
        assertEquals(1, msg.build(fonts).records.size)
    }
}
