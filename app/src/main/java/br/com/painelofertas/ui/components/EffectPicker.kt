package br.com.painelofertas.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.painelofertas.render.PanelBitmap
import br.com.painelofertas.ui.theme.PanelBg

/** Os efeitos globais que o painel entende (comando `ONLINE=<índice>`). */
enum class PanelEffect(val index: Int, val label: String, val descricao: String) {
    PADRAO(0, "Padrão", "A tela aparece e fica firme."),
    PISCA_INVERTE(1, "Pisca / Inverte", "Alterna o texto com o negativo — chama muita atenção."),
    PISCA_PADRAO(2, "Pisca / Padrão", "A tela pisca acendendo e apagando."),
}

/**
 * Escolha de efeito **com prévia ao vivo**: cada opção mostra uma mini-placa de LED
 * animada com o próprio efeito, para o lojista ver antes de mandar ao painel.
 */
@Composable
fun EffectPicker(
    amostra: PanelBitmap,
    selecionado: Int,
    litColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PanelEffect.entries.forEach { efeito ->
            EffectOption(
                efeito = efeito,
                amostra = amostra,
                litColor = litColor,
                selecionado = selecionado == efeito.index,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(efeito.index) },
            )
        }
    }
}

@Composable
private fun EffectOption(
    efeito: PanelEffect,
    amostra: PanelBitmap,
    litColor: androidx.compose.ui.graphics.Color,
    selecionado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    // Ciclo comum a todos: 0..1 contínuo, do qual cada efeito tira o que precisa.
    val ciclo by rememberInfiniteTransition(label = "efeito").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "cicloEfeito",
    )
    val invertido = efeito == PanelEffect.PISCA_INVERTE && ciclo > 0.5f
    val alfa = if (efeito == PanelEffect.PISCA_PADRAO && ciclo > 0.5f) 0.12f else 1f

    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selecionado) cs.secondaryContainer else cs.surfaceContainerHigh)
            .border(
                width = if (selecionado) 2.dp else 1.dp,
                color = if (selecionado) cs.primary else cs.outlineVariant,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(7.dp)).background(PanelBg),
            contentAlignment = Alignment.Center,
        ) {
            PanelPreview(
                amostra,
                Modifier.fillMaxWidth().height(38.dp).padding(2.dp).graphicsLayer { this.alpha = alfa },
                litColor = litColor,
                showOff = false,
                inverted = invertido,
            )
        }
        Text(
            efeito.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal,
            color = if (selecionado) cs.onSecondaryContainer else cs.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
