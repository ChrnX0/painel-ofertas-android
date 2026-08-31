package br.com.painelofertas.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Os acentos pastéis do app, em **duas versões**.
 *
 * O mesmo pastel não serve para os dois temas: `#8AB4F8` brilha sobre carvão e
 * some sobre branco (contraste ~1,8:1 — ilegível). O tema claro usa o **mesmo
 * matiz**, mais fundo e um pouco mais saturado, de modo que continue macio mas
 * apareça. Como a identidade é o matiz, e não o brilho, o app parece o mesmo nos
 * dois temas sem nenhuma tela ficar ilegível.
 *
 * O [Accent] público lê daqui via `LocalAccents`, então cada uso
 * (`tint = Accent.Blue`) já se adapta sozinho — não há dois nomes para a mesma cor.
 */
data class AccentPalette(
    val blue: Color,
    val green: Color,
    val amber: Color,
    val lilac: Color,
    val rose: Color,
    val teal: Color,
    val peach: Color,
    val mint: Color,
    val sky: Color,
    val gray: Color,
)

/** Escuro: pastéis claros, que acendem sobre o carvão. */
val AccentsDark = AccentPalette(
    blue = Color(0xFF8AB4F8),
    green = Color(0xFF8FE0BF),
    amber = Color(0xFFF3D08A),
    lilac = Color(0xFFC3B0F5),
    rose = Color(0xFFF2AEC6),
    teal = Color(0xFF8FD9D2),
    peach = Color(0xFFF6BFA0),
    mint = Color(0xFFA8E6A3),
    sky = Color(0xFF9BD5F0),
    gray = Color(0xFF9AA6B6),
)

/**
 * Claro: mesmo matiz, ~35% mais fundo. Ainda pastel (nada de cor pura e berrante),
 * mas com contraste suficiente para ícone e texto sobre superfície clara.
 */
val AccentsLight = AccentPalette(
    blue = Color(0xFF3B6FBF),
    green = Color(0xFF2E8F6B),
    amber = Color(0xFF9C7526),
    lilac = Color(0xFF6E5CB8),
    rose = Color(0xFFB55A80),
    teal = Color(0xFF2C8078),
    peach = Color(0xFFB56A3E),
    mint = Color(0xFF4A8C46),
    sky = Color(0xFF2F7FA6),
    gray = Color(0xFF6B7787),
)

val LocalAccents = staticCompositionLocalOf { AccentsDark }

/** O tema é claro? Usado onde a intensidade do efeito muda (aurora, tinturas). */
val LocalIsLight = staticCompositionLocalOf { false }
