package br.com.painelofertas.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * Entrada escalonada (fade + leve deslize de baixo pra cima), no espírito das
 * animações do One UI. Envolva cada cartão/seção com um [delayMillis] crescente
 * e a tela "monta" em cascata em vez de aparecer seca. Barato: anima só na
 * primeira composição.
 */
@Composable
fun Appear(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        shown = true
    }
    AnimatedVisibility(
        visible = shown,
        modifier = modifier,
        enter = fadeIn(tween(280)) +
            slideInVertically(
                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
            ) { full -> full / 6 },
    ) { content() }
}
