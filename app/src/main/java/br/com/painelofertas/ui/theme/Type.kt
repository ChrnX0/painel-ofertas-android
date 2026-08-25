package br.com.painelofertas.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import br.com.painelofertas.R

/*
 * Archivo (fonte variável) para a interface — sotaque industrial, sem cara de
 * Roboto genérico. IBM Plex Mono para dados técnicos (IP, %, versão, CRC),
 * dando aquele ar de "instrumento".
 */

@OptIn(ExperimentalTextApi::class)
private fun archivo(weight: Int) = Font(
    resId = R.font.archivo_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Archivo = FontFamily(
    archivo(400),
    archivo(500),
    archivo(600),
    archivo(700),
    archivo(800),
)

/** Fonte monoespaçada para leituras técnicas. */
val Mono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

private val base = Typography()

val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Archivo, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp),
    displayMedium = base.displayMedium.copy(fontFamily = Archivo, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    displaySmall = base.displaySmall.copy(fontFamily = Archivo, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Archivo, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontFamily = Archivo, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = base.headlineSmall.copy(fontFamily = Archivo, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    titleLarge = base.titleLarge.copy(fontFamily = Archivo, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleMedium = base.titleMedium.copy(fontFamily = Archivo, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = Archivo, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontFamily = Archivo),
    bodyMedium = base.bodyMedium.copy(fontFamily = Archivo),
    bodySmall = base.bodySmall.copy(fontFamily = Archivo),
    labelLarge = base.labelLarge.copy(fontFamily = Archivo, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontFamily = Archivo, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontFamily = Archivo, fontWeight = FontWeight.Medium),
)
