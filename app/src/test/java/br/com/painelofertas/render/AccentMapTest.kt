package br.com.painelofertas.render

import br.com.painelofertas.protocol.AccentMap
import org.junit.Assert.assertEquals
import org.junit.Test

class AccentMapTest {

    @Test
    fun uppercases_and_maps_accents_to_slots() {
        // "Promoção" -> "PROMOÇÃO" -> Ç='i', Ã='b' (placeholders minúsculos)
        assertEquals("PROMOibO", AccentMap.normalize("Promoção"))
    }

    @Test
    fun out_of_range_becomes_space() {
        // '~' (126) está fora de 32..108 -> espaço
        assertEquals("A B", AccentMap.normalize("a~b"))
    }

    @Test
    fun denormalize_is_inverse_for_accents() {
        val slots = AccentMap.normalize("AÇÃO")
        assertEquals("AÇÃO", AccentMap.denormalize(slots))
    }
}
