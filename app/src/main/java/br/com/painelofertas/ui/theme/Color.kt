package br.com.painelofertas.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Paleta "console escuro" da LedBlock.
 * O azul da marca é o ACENTO (não a cara inteira); o chrome é carvão azulado.
 * O secondaryContainer é azulado de propósito — é o que mata o lavanda padrão
 * do Material nos chips selecionados e no indicador da barra de navegação.
 */

// --- Identidade ---
val LedBlue = Color(0xFF1E88E5)
val LedBlueBright = Color(0xFF4AA3FF)
val LedBlueDeep = Color(0xFF1565C0)

// ===== TEMA ESCURO (principal) =====
// As superfícies têm um leve tom azul-arroxeado (chroma sutil) em vez de cinza
// puro — dá vida ao tema sem sair do pastel, no espírito do One UI.
val DarkPrimary = Color(0xFF8FBCF5)   // azul pastel (botões/acentos) — suave, sem berrar
val DarkOnPrimary = Color(0xFF06192B)
val DarkPrimaryContainer = Color(0xFF1B5285)
val DarkOnPrimaryContainer = Color(0xFFD9EBFF)
val DarkSecondary = Color(0xFFB3C0D6)
val DarkOnSecondary = Color(0xFF12202E)
val DarkSecondaryContainer = Color(0xFF1B4570)   // ← chips/nav selecionados (era lavanda)
val DarkOnSecondaryContainer = Color(0xFFDCEBFF)
val DarkTertiary = Color(0xFFF3D08A)             // âmbar pastel (combina com o LED)
val DarkOnTertiary = Color(0xFF3A2600)
val DarkTertiaryContainer = Color(0xFF57401A)
val DarkOnTertiaryContainer = Color(0xFFFFE9BE)
val DarkBackground = Color(0xFF0C1017)
val DarkOnBackground = Color(0xFFE9EEF6)
val DarkSurface = Color(0xFF141922)
val DarkOnSurface = Color(0xFFE9EEF6)
val DarkSurfaceVariant = Color(0xFF232B39)
val DarkOnSurfaceVariant = Color(0xFFB2BFD1)
val DarkSurfaceContainerLowest = Color(0xFF0A0D13)
val DarkSurfaceContainerLow = Color(0xFF141922)
val DarkSurfaceContainer = Color(0xFF191F2A)
val DarkSurfaceContainerHigh = Color(0xFF202734)
val DarkSurfaceContainerHighest = Color(0xFF28303F)
val DarkOutline = Color(0xFF445061)
val DarkOutlineVariant = Color(0xFF2A3341)
val DarkError = Color(0xFFFF6B7E)
val DarkOnError = Color(0xFF40060E)
val DarkErrorContainer = Color(0xFF5C1420)
val DarkOnErrorContainer = Color(0xFFFFD9DE)

// ===== TEMA CLARO =====
val LightPrimary = Color(0xFF1565C0)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFD6E9FB)
val LightOnPrimaryContainer = Color(0xFF082A47)
val LightSecondary = Color(0xFF4C5B6B)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFD2E4F7)  // ← chips/nav selecionados azulados
val LightOnSecondaryContainer = Color(0xFF0C3A5E)
val LightTertiary = Color(0xFFA9670A)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFDEA8)
val LightOnTertiaryContainer = Color(0xFF351F00)
// Claro: fundo levemente azulado (não branco puro) para o pastel dos cartões
// aparecer — mesma lógica do escuro, do outro lado.
val LightBackground = Color(0xFFEBF1F9)
val LightOnBackground = Color(0xFF101826)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF101826)
val LightSurfaceVariant = Color(0xFFDFE7F2)
val LightOnSurfaceVariant = Color(0xFF47546A)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF5F9FE)
val LightSurfaceContainer = Color(0xFFEEF3FB)
val LightSurfaceContainerHigh = Color(0xFFE6EDF7)
val LightSurfaceContainerHighest = Color(0xFFDDE7F3)
val LightOutline = Color(0xFFB6C1CE)
val LightOutlineVariant = Color(0xFFD5DEE8)
val LightError = Color(0xFFBA1A2B)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDADE)
val LightOnErrorContainer = Color(0xFF410008)

// ===== Placa de LED (independe do tema — é hardware físico preto) =====
val PanelBg = Color(0xFF05070B)

/** Cores selecionáveis do LED na prévia (índice salvo em SettingsStore.ledColor). */
val LedColors = listOf(
    Color(0xFFFFB300), // 0 Âmbar
    Color(0xFFFF453A), // 1 Vermelho
    Color(0xFF2BE06A), // 2 Verde
    Color(0xFF45B4FF), // 3 Azul
    Color(0xFFFFFFFF), // 4 Branco
)
val LedColorNames = listOf("Âmbar", "Vermelho", "Verde", "Azul", "Branco")

fun ledColorAt(index: Int): Color = LedColors.getOrElse(index) { LedColors[0] }
