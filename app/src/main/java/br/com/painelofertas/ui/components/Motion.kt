package br.com.painelofertas.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

/**
 * Vocabulário de movimento do app — **molas, não curvas de tempo**.
 *
 * Uma `tween` percorre uma duração fixa e para seca: parece um slide de
 * apresentação. Uma mola tem massa e atrito, então acelera, passa um pouco do
 * ponto e assenta — é como as coisas se movem no mundo, e é o que dá a sensação
 * "orgânica". Também é interrompível: se o usuário tocar de novo no meio da
 * animação, a mola muda de destino a partir da **velocidade atual**, sem cortar.
 *
 * Três tokens, para o app inteiro falar a mesma língua:
 * - [gentle] transições de conteúdo e cor — assenta sem oscilar
 * - [bouncy] seleção e entrada de elementos — dá aquele "pop" do One UI
 * - [snappy] resposta imediata ao dedo (pressão, arraste)
 */
object Motion {
    fun <T> gentle(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    fun <T> bouncy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)

    fun <T> snappy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)

    /** Bem elástica — só para o que deve chamar atenção (o botão principal). */
    fun <T> springy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessLow)
}

/**
 * Encolhe levemente enquanto o dedo está em cima e volta com mola ao soltar.
 *
 * É a interação mais barata que existe e a que mais muda a percepção: o elemento
 * deixa de ser um desenho e passa a ser algo que **cede** ao toque. Use a mesma
 * [interactionSource] do `clickable` para que pressão e ação sejam o mesmo gesto.
 */
fun Modifier.pressBounce(
    interactionSource: MutableInteractionSource,
    scaleDown: Float = 0.965f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) scaleDown else 1f,
        Motion.snappy(),
        label = "pressBounce",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * Entrada escalonada: sobe, cresce um tiquinho e assenta com mola. Envolva cada
 * cartão com um [delayMillis] crescente e a tela **se monta** em cascata em vez
 * de aparecer pronta. Anima só na primeira composição.
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
        enter = fadeIn(tween(300)) +
            scaleIn(Motion.bouncy(), initialScale = 0.94f) +
            slideInVertically(Motion.bouncy()) { full -> full / 7 },
    ) { content() }
}
