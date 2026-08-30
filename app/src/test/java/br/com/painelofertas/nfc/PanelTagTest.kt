package br.com.painelofertas.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Formato da etiqueta NFC de painel. Precisa ser tolerante (etiquetas gravadas por
 * versões futuras continuam legíveis) e **seletivo** (não reagir a crachás e
 * cartões de outros sistemas que o celular encoste por acaso).
 */
class PanelTagTest {

    @Test
    fun `le etiqueta completa`() {
        val t = PanelTag.parse("lb=1;id=07;nome=Vitrine da Frente;ip=192.168.0.42;grupo=Acougue;modelo=LB-96;fw=1.2")
        assertNotNull(t)
        assertEquals("07", t!!.id)
        assertEquals("Vitrine da Frente", t.nome)
        assertEquals("192.168.0.42", t.ip)
        assertEquals("Acougue", t.grupo)
        assertEquals("LB-96", t.modelo)
        assertEquals("1.2", t.firmware)
    }

    @Test
    fun `le etiqueta minima`() {
        val t = PanelTag.parse("lb=1;id=03")
        assertNotNull(t)
        assertEquals("03", t!!.id)
    }

    @Test
    fun `aceita formato de URI`() {
        val t = PanelTag.parse("ledblock://painel?lb=1&id=09&ip=192.168.0.51")
        assertNotNull(t)
        assertEquals("09", t!!.id)
        assertEquals("192.168.0.51", t.ip)
    }

    @Test
    fun `ignora etiqueta que nao e da LedBlock`() {
        assertNull(PanelTag.parse("id=07;nome=Cracha do Joao"))
        assertNull(PanelTag.parse("https://exemplo.com/promo"))
        assertNull(PanelTag.parse("texto qualquer"))
        assertNull(PanelTag.parse(""))
    }

    @Test
    fun `ignora etiqueta LedBlock sem dado util`() {
        assertNull(PanelTag.parse("lb=1;modelo=LB-96"))
    }

    @Test
    fun `tolera espacos ordem trocada e chaves desconhecidas`() {
        val t = PanelTag.parse(" grupo = Hortifruti ; lb=1 ; recurso_novo=xyz ; id = 12 ")
        assertNotNull(t)
        assertEquals("12", t!!.id)
        assertEquals("Hortifruti", t.grupo)
    }

    @Test
    fun `gravar e ler de volta preserva os dados`() {
        val original = PanelTag(id = "07", nome = "Balcao", ip = "10.0.0.5", grupo = "Acougue", modelo = "LB-96", firmware = "2.0")
        val lido = PanelTag.parse(original.toTagText())
        assertEquals(original, lido)
    }
}
