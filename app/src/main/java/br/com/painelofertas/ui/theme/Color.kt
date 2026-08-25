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
val DarkPrimary = Color(0xFF4AA3FF)
val DarkOnPrimary = Color(0xFF042138)
val DarkPrimaryContainer = Color(0xFF124A7C)
val DarkOnPrimaryContainer = Color(0xFFD3E8FF)
val DarkSecondary = Color(0xFFA7B7C9)
val DarkOnSecondary = Color(0xFF12202E)
val DarkSecondaryContainer = Color(0xFF143A5C)   // ← chips/nav selecionados (era lavanda)
val DarkOnSecondaryContainer = Color(0xFFCFE6FF)
val DarkTertiary = Color(0xFFFFB020)
val DarkOnTertiary = Color(0xFF3A2600)
val DarkTertiaryContainer = Color(0xFF4A3410)
val DarkOnTertiaryContainer = Color(0xFFFFE0A6)
val DarkBackground = Color(0xFF0B0E13)
val DarkOnBackground = Color(0xFFE7ECF3)
val DarkSurface = Color(0xFF12161D)
val DarkOnSurface = Color(0xFFE7ECF3)
val DarkSurfaceVariant = Color(0xFF1E2530)
val DarkOnSurfaceVariant = Color(0xFFA9B6C6)
val DarkSurfaceContainerLowest = Color(0xFF090C11)
val DarkSurfaceContainerLow = Color(0xFF12161D)
val DarkSurfaceContainer = Color(0xFF161B23)
val DarkSurfaceContainerHigh = Color(0xFF1C222C)
val DarkSurfaceContainerHighest = Color(0xFF232A35)
val DarkOutline = Color(0xFF3A4552)
val DarkOutlineVariant = Color(0xFF232B35)
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
val LightBackground = Color(0xFFEEF2F8)
val LightOnBackground = Color(0xFF101826)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF101826)
val LightSurfaceVariant = Color(0xFFE4EAF2)
val LightOnSurfaceVariant = Color(0xFF47546A)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF6F9FD)
val LightSurfaceContainer = Color(0xFFF0F4FA)
val LightSurfaceContainerHigh = Color(0xFFE9EFF7)
val LightSurfaceContainerHighest = Color(0xFFE2EAF3)
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
