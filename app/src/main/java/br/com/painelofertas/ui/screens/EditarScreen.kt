package br.com.painelofertas.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.painelofertas.editor.FrameDraft
import br.com.painelofertas.editor.LineDraft
import br.com.painelofertas.protocol.Album
import br.com.painelofertas.protocol.DurationTable
import br.com.painelofertas.protocol.PanelFont
import br.com.painelofertas.render.OfertaSpec
import br.com.painelofertas.render.PanelRenderer
import br.com.painelofertas.ui.components.ButtonShape
import br.com.painelofertas.ui.components.LedBezel
import br.com.painelofertas.ui.components.MonoText
import br.com.painelofertas.ui.components.PanelPreview
import br.com.painelofertas.ui.components.SectionLabel
import br.com.painelofertas.ui.components.SegChoice
import br.com.painelofertas.ui.LocalSnackbar
import br.com.painelofertas.ui.rememberContainer
import br.com.painelofertas.ui.theme.ledColorAt
import kotlinx.coroutines.launch
import br.com.painelofertas.ui.vm.AppViewModel
import br.com.painelofertas.ui.vm.EditorViewModel

@Composable
fun EditarScreen() {
    val container = rememberContainer()
    val fonts = container.fonts
    val scroll = rememberScrollState()
    val vm: EditorViewModel = viewModel()
    val haptic = LocalHapticFeedback.current
    val snackbar = LocalSnackbar.current
    val editScope = rememberCoroutineScope()
    val nav: AppViewModel = viewModel()
    var msg by remember { mutableStateOf("") }
    val albuns by container.albums.names.collectAsState()
    var confirmOverwrite by remember { mutableStateOf(false) }

    val cur = vm.current

    fun doSave() {
        container.albums.save(vm.toAlbum(fonts))
        msg = "Álbum \"${vm.nome}\" salvo (${vm.frames.size} quadros)."
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        // ===== 1. PRÉVIA =====
        if (cur != null) {
            PreviewCard(cur.build(fonts, vm.portrait), cur.halfScreen, vm.portrait)
            SegChoice(listOf("Horizontal", "Vertical"), if (vm.portrait) 1 else 0, Modifier.fillMaxWidth()) { vm.portrait = it == 1 }
        }

        // ===== 2. CONTEÚDO (o que aparece no painel) =====
        when (val d = cur) {
            is FrameDraft.Msg -> MsgForm(d) { vm.replaceSel(it) }
            is FrameDraft.Ofe -> OfeForm(d) { vm.replaceSel(it) }
            is FrameDraft.Raw -> Text(
                "Oferta salva — preview e reordenação disponíveis. Para editar campo-a-campo, crie uma nova Oferta.",
                style = MaterialTheme.typography.bodySmall,
            )
            null -> {}
        }

        // ===== 3. SEQUÊNCIA (telas do álbum + tempo) =====
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Sequência · ${vm.frames.size} ${if (vm.frames.size == 1) "tela" else "telas"}")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    vm.frames.forEachIndexed { i, d ->
                        FilterChip(selected = i == vm.sel, onClick = { vm.selected = i }, label = { Text("${i + 1} · ${d.label()}") })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { vm.addMsg() }, shape = ButtonShape) { Icon(Icons.Filled.Add, null); Text("Mensagem") }
                    OutlinedButton(onClick = { vm.addOfe() }, shape = ButtonShape) { Icon(Icons.Filled.Add, null); Text("Oferta") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { vm.moveUp() }) { Icon(Icons.Filled.KeyboardArrowUp, "Subir") }
                    IconButton(onClick = { vm.moveDown() }) { Icon(Icons.Filled.KeyboardArrowDown, "Descer") }
                    IconButton(onClick = { vm.duplicate() }) { Icon(Icons.Filled.ContentCopy, "Duplicar quadro") }
                    IconButton(enabled = vm.frames.size > 1, onClick = {
                    vm.delete()
                    editScope.launch {
                        val r = snackbar.showSnackbar("Quadro excluído", "Desfazer", withDismissAction = true)
                        if (r == SnackbarResult.ActionPerformed) vm.undoDelete()
                    }
                }) { Icon(Icons.Filled.Delete, "Excluir quadro") }
                }
                if (cur != null && cur !is FrameDraft.Raw) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SectionLabel("Tempo desta tela")
                    TempoSelector(cur.durationIndex) { vm.setDuration(it) }
                }
            }
        }

        // ===== 4. ÁLBUM (salvar / abrir / enviar) =====
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Álbum")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(vm.nome, { vm.nome = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (albuns.contains(vm.nome)) confirmOverwrite = true else doSave()
                        },
                        shape = ButtonShape,
                    ) { Text("Salvar") }
                }
                OutlinedButton(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); doSave(); nav.goToSend(vm.nome) },
                    shape = ButtonShape,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Salvar e enviar  →") }
                if (albuns.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Abrir:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        albuns.forEach { a ->
                            AssistChip(onClick = {
                                container.albums.load(a)?.let { vm.load(it); msg = "Aberto \"${it.name}\"." }
                            }, label = { Text(a) })
                        }
                    }
                }
            }
        }

        if (msg.isNotBlank()) Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }

    if (confirmOverwrite) {
        AlertDialog(
            onDismissRequest = { confirmOverwrite = false },
            title = { Text("Sobrescrever álbum?") },
            text = { Text("Já existe um álbum chamado \"${vm.nome}\". Deseja substituir?") },
            confirmButton = { TextButton(onClick = { confirmOverwrite = false; doSave() }) { Text("Substituir") } },
            dismissButton = { TextButton(onClick = { confirmOverwrite = false }) { Text("Cancelar") } },
        )
    }
}

// ---------- helpers ----------

// Dimensões do painel (colunas x linhas). Em retrato, largura e altura trocam.
private fun panelDims(halfScreen: Boolean, portrait: Boolean): Pair<Int, Int> {
    val (w, h) = if (halfScreen) 96 to 92 else 188 to 92
    return if (portrait) h to w else w to h
}

/** Formata os dígitos crus como "R$ 9,90" (espelha a lógica do OfertaLayout). */
private fun formatPreco(valor: String, cents3: Boolean, centsOff: Boolean): String {
    val digits = valor.filter { it.isDigit() }
    if (digits.isEmpty()) return "R$ 0,00"
    if (centsOff) return "R$ " + (digits.trimStart('0').ifEmpty { "0" })
    val n = if (cents3) 3 else 2
    val padded = digits.padStart(n + 1, '0')
    val reais = padded.dropLast(n).trimStart('0').ifEmpty { "0" }
    val cents = padded.takeLast(n)
    return "R$ $reais,$cents"
}

@Composable
private fun PreviewCard(frame: br.com.painelofertas.protocol.PanelFrame, halfScreen: Boolean, portrait: Boolean) {
    val container = rememberContainer()
    val fonts = container.fonts
    val (cols, rows) = panelDims(halfScreen, portrait)
    val ledIdx by container.settings.ledColor.collectAsState()
    val led = ledColorAt(ledIdx)
    val bmp = remember(frame, cols, rows) { PanelRenderer.renderFrame(frame, cols, rows, fonts) }

    // respiro sutil do brilho (barato: anima só o alfa da camada, sem redesenhar)
    val pulse by rememberInfiniteTransition(label = "led").animateFloat(
        initialValue = 0.90f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "ledPulse",
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LedBezel(Modifier.fillMaxWidth(), boardHeight = 138.dp) {
            PanelPreview(
                bmp,
                Modifier.fillMaxSize().padding(10.dp).graphicsLayer { alpha = pulse },
                litColor = led,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(led))
            MonoText(
                "PRÉVIA AO VIVO · ${if (halfScreen) "MEIA" else "CHEIA"}" +
                    (if (portrait) " · VERTICAL" else " · HORIZONTAL") + " · ${cols}×$rows",
                size = 10,
            )
        }
    }
}

@Composable
private fun TempoSelector(index: Int, onChange: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DurationTable.secondsByIndex.forEachIndexed { i, seg ->
            FilterChip(
                selected = index == i,
                onClick = { onChange(i) },
                label = { Text(if (i == 0) "Auto" else "${seg}s", style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}

/** Linha de interruptor: rótulo à esquerda, switch à direita, largura cheia. */
@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked, onChange)
    }
}

@Composable
private fun MsgForm(draft: FrameDraft.Msg, onChange: (FrameDraft.Msg) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card {
            Column {
                ToggleRow("Meia tela", draft.halfScreen) { onChange(draft.copy(halfScreen = it)) }
                HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                ToggleRow("Habilitar quadro", draft.enabled) { onChange(draft.copy(enabled = it)) }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionLabel("Borda")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Sem", "Segmentada", "Contínua").forEachIndexed { i, nome ->
                    FilterChip(draft.border == i, { onChange(draft.copy(border = i)) }, { Text(nome, style = MaterialTheme.typography.labelSmall) })
                }
            }
        }

        SectionLabel("Linhas de texto")
        draft.lines.forEachIndexed { i, linha ->
            LineCard(linha,
                onChange = { nl -> onChange(draft.copy(lines = draft.lines.toMutableList().also { it[i] = nl })) },
                onRemove = { onChange(draft.copy(lines = draft.lines.toMutableList().also { if (it.size > 1) it.removeAt(i) })) },
            )
        }
        OutlinedButton(onClick = { onChange(draft.copy(lines = draft.lines + LineDraft())) }, shape = ButtonShape) {
            Icon(Icons.Filled.Add, null); Text("Linha")
        }
    }
}

@Composable
private fun LineCard(linha: LineDraft, onChange: (LineDraft) -> Unit, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(linha.text, { onChange(linha.copy(text = it)) }, label = { Text("Texto") }, singleLine = true, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, "Remover") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("Linha", linha.row) { onChange(linha.copy(row = it)) }
                NumField("Coluna", linha.col) { onChange(linha.copy(col = it)) }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PanelFont.entries.forEach { f ->
                    FilterChip(linha.font == f.code, { onChange(linha.copy(font = f.code)) }, { Text(f.displayName, style = MaterialTheme.typography.labelSmall) })
                }
            }
        }
    }
}

@Composable
private fun OfeForm(draft: FrameDraft.Ofe, onChange: (FrameDraft.Ofe) -> Unit) {
    val s = draft.spec
    fun set(ns: OfertaSpec) = onChange(draft.copy(spec = ns))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // modelos rápidos: preenchem cabeçalho/medida/rodapé de uma vez
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionLabel("Modelos")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MODELOS.forEach { m ->
                    AssistChip(
                        onClick = { set(s.copy(cabecalho = m.cabecalho, medida = m.medida, rodape = m.rodape, subtituloAtivo = false)) },
                        label = { Text(m.nome) },
                    )
                }
            }
        }

        // interruptores agrupados
        Card {
            Column {
                ToggleRow("Meia tela", draft.halfScreen) { onChange(draft.copy(halfScreen = it)) }
                HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                ToggleRow("Habilitar quadro", s.enabled) { set(s.copy(enabled = it)) }
            }
        }

        // Topo: cabeçalho OU (título + subtítulo) — excludentes, igual ao painel.
        if (!s.subtituloAtivo) {
            OutlinedTextField(s.cabecalho, { set(s.copy(cabecalho = it)) }, label = { Text("Cabeçalho") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        OutlinedTextField(s.titulo, { set(s.copy(titulo = it)) }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Card(Modifier.fillMaxWidth()) {
            Column {
                ToggleRow("Usar subtítulo", s.subtituloAtivo) { set(s.copy(subtituloAtivo = it)) }
                Text(
                    if (s.subtituloAtivo) "Com subtítulo, o painel mostra Título + Subtítulo — o cabeçalho fica oculto."
                    else "Ative para trocar o cabeçalho por um subtítulo abaixo do título.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                )
            }
        }
        if (s.subtituloAtivo) {
            OutlinedTextField(s.subtitulo, { set(s.copy(subtitulo = it)) }, label = { Text("Subtítulo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }

        // === PREÇO (herói do formulário) ===
        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Preço")
                OutlinedTextField(
                    s.valor, { set(s.copy(valor = it)) },
                    label = { Text("Dígitos (990 = 9,90)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    Text("=", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MonoText(
                        formatPreco(s.valor, s.centavos3Casas, s.centavosDesligados),
                        size = 20,
                        weight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(s.centavosReduzidos, { set(s.copy(centavosReduzidos = !s.centavosReduzidos)) }, { Text("Reduzidos") })
                    FilterChip(s.centavos3Casas, { set(s.copy(centavos3Casas = !s.centavos3Casas)) }, { Text("3 casas") })
                    FilterChip(s.centavosDesligados, { set(s.copy(centavosDesligados = !s.centavosDesligados)) }, { Text("Sem centavos") })
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(s.medida, { set(s.copy(medida = it)) }, label = { Text("Medida") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(s.auxiliar, { set(s.copy(auxiliar = it)) }, label = { Text("Auxiliar") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(s.rodape, { set(s.copy(rodape = it)) }, label = { Text("Rodapé") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun NumField(label: String, value: Int, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { onChange(it.toIntOrNull() ?: 0) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.width(110.dp),
    )
}

/** Modelos rápidos de oferta para o lojista (cabeçalho/medida/rodapé). */
private data class OfertaModelo(val nome: String, val cabecalho: String, val medida: String, val rodape: String)

private val MODELOS = listOf(
    OfertaModelo("Açougue", "OFERTA", "O KILO", "OFERTA VÁLIDA HOJE"),
    OfertaModelo("Hortifruti", "HORTIFRUTI", "O KILO", ""),
    OfertaModelo("Bebidas", "OFERTA", "A UNIDADE", ""),
    OfertaModelo("Padaria", "PADARIA", "O KILO", ""),
    OfertaModelo("Frios", "OFERTA", "100 G", ""),
    OfertaModelo("Limpeza", "OFERTA", "CADA", ""),
)
