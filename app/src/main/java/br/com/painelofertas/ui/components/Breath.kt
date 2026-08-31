package br.com.painelofertas.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * **O fôlego do app** — um único oscilador, de 0 a 1 e de volta, que todos os
 * elementos vivos compartilham.
 *
 * Este é o ponto: se cada elemento tivesse a própria animação de respiro, eles
 * pulsariam fora de fase e o resultado seria *tremor*, não vida — um monte de
 * peças se mexendo cada uma para o seu lado. Com um pulmão só, a aurora, o brilho
 * do painel, o contorno dos cartões e o botão Publicar incham e murcham **juntos**,
 * e o app passa a ler como um organismo em vez de uma colagem.
 *
 * ### Como usar sem pagar caro
 * Leia `LocalBreath.current.value` **dentro** de uma lambda de `graphicsLayer`,
 * `drawBehind` ou `drawWithContent`. Estado lido dentro dessas lambdas é lido na
 * **fase de desenho**: o Compose reexecuta só o desenho daquele nó, pulando
 * recomposição e medição. Ler fora (direto no corpo do @Composable) recomporia a
 * árvore inteira 15 vezes por segundo — que é exatamente o erro a evitar.
 */
val LocalBreath = staticCompositionLocalOf<State<Float>> {
    error("LocalBreath não fornecido — envolva a UI em BreathProvider")
}

/** Período do ciclo, em ms. ~4,2 s é o ritmo de uma respiração calma. */
private const val PERIODO_MS = 4200f

/**
 * Passo do relógio. Respiração é lenta: a 15 quadros por segundo a escala muda
 * ~0,05% por passo, imperceptível, e custa 4× menos que os 60 do sistema.
 */
private const val TICK_MS = 66L

/** Envolva a raiz da UI. Fornece [LocalBreath] para tudo que estiver dentro. */
@Composable
fun BreathProvider(content: @Composable () -> Unit) {
    val breath = remember { mutableFloatStateOf(0f) }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(TICK_MS)
            t += TICK_MS
            // seno de 0..1: inspira e expira sem canto vivo nos extremos
            breath.floatValue = (sin(2f * Math.PI.toFloat() * (t % PERIODO_MS) / PERIODO_MS) + 1f) / 2f
        }
    }
    CompositionLocalProvider(LocalBreath provides breath, content = content)
}

/**
 * Mistura o fôlego numa faixa: `respirar(0.98f, 1.05f)` devolve um número que vai
 * e volta entre os dois. Chame dentro de lambdas de desenho (ver [LocalBreath]).
 */
fun Float.entre(min: Float, max: Float) = min + (max - min) * this
