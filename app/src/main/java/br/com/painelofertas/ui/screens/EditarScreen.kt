package br.com.painelofertas.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.painelofertas.data.PanelStatus
import br.com.painelofertas.editor.FrameDraft
import br.com.painelofertas.editor.LineDraft
import br.com.painelofertas.net.PanelLink
import br.com.painelofertas.net.UdpLink
import br.com.painelofertas.protocol.Album
import br.com.painelofertas.protocol.DurationTable
import br.com.painelofertas.protocol.PanelFont
import br.com.painelofertas.render.OfertaSpec
import br.com.painelofertas.render.PanelRenderer
import br.com.painelofertas.ui.components.Appear
import br.com.painelofertas.ui.components.ButtonShape
import br.com.painelofertas.ui.components.CardHeader
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
    val panels by container.panels.panels.collectAsState()
    val usbConnected by container.usb.connected.collectAsState()
    var confirmOverwrite by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var panelBusy by remember { mutableStateOf(false) }

    val cur = vm.current

    fun doSave() {
        container.albums.save(vm.toAlbum(fonts))
        msg = "Álbum \"${vm.nome}\" salvo (${vm.frames.size} telas)."
    }

    // Alvo das ações de painel (sincronizar / limpar): prefere um painel online
    // na rede; se não houver, usa o USB conectado.
    val onlinePanel = panels.firstOrNull { it.status == PanelStatus.ONLINE }
    fun viaUsb() = onlinePanel == null && usbConnected
    fun panelLink(): PanelLink? = when {
        onlinePanel != null -> UdpLink(onlinePanel.ip, container.udp)
        usbConnected -> container.usb.link.value
        else -> null
    }
    val temPainel = onlinePanel != null || usbConnected

    fun sincronizar() {
        val link = panelLink() ?: run { msg = "Nenhum painel conectado. Abra a aba Painéis para localizar."; return }
        panelBusy = true; msg = "Lendo o painel…"
        val usb = viaUsb()
        container.connection.transferStarted(usb)
        editScope.launch {
            val album = runCatching { container.downloadAlbum(link) }.getOrNull()
            container.connection.transferEnded(usb, album != null)
            if (album == null) msg = "Não consegui ler o painel (verifique a conexão)."
            else if (album.frames.isEmpty()) msg = "O painel está vazio — nada para sincronizar."
            else {
                vm.mergePanelFrames(album)
                msg = "Sincronizado: ${album.frames.size} tela(s) do painel. Elas entraram na sequência — " +
                    "selecione qualquer uma para excluir, reordene e insira a sua onde quiser."
            }
            panelBusy = false
        }
    }

    fun limpar() {
        val link = panelLink() ?: run { msg = "Nenhum painel conectado."; return }
        panelBusy = true; msg = "Limpando o painel…"
        val usb = viaUsb()
        container.connection.transferStarted(usb)
        editScope.launch {
            val ok = runCatching { container.clearPanel(link, onlinePanel?.brightness ?: 100) }.getOrDefault(false)
            container.connection.transferEnded(usb, ok)
            msg = if (ok) "✅ Painel limpo." else "❌ Falha ao limpar o painel."
            panelBusy = false
        }
    }

    // Exportar/Importar .alb para um arquivo escolhido pelo usuário (interop com o
    // app Windows e backup) — via Storage Access Framework.
    val ctx = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            val ok = runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(vm.toAlbum(fonts).toAlbText().toByteArray(Charsets.ISO_8859_1)) }
            }.isSuccess
            msg = if (ok) "Álbum exportado para arquivo." else "Falha ao exportar."
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val text = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.ISO_8859_1) } ?: ""
                val album = Album.fromAlbText(text)
                container.albums.save(album)
                vm.load(album)
                msg = "Importado \"${album.name}\"."
            }.onFailure { msg = "Falha ao importar (arquivo .alb inválido?)." }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        Appear {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SectionLabel("Editor de telas")
                Text("Montar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            }
        }

        // ===== 1. PRÉVIA =====
        if (cur != null) {
            Appear(delayMillis = 50) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PreviewCard(cur.build(fonts, vm.portrait), cur.halfScreen, vm.portrait)
                    SegChoice(listOf("Horizontal", "Vertical"), if (vm.portrait) 1 else 0, Modifier.fillMaxWidth()) { vm.portrait = it == 1 }
                }
            }
        }

        // ===== 2. CONTEÚDO (o que aparece no painel) =====
        Appear(delayMillis = 100) {
            when (val d = cur) {
                is FrameDraft.Msg -> MsgForm(d) { vm.replaceSel(it) }
                is FrameDraft.Ofe -> OfeForm(d) { vm.replaceSel(it) }
                is FrameDraft.Raw -> Text(
                    "Oferta salva — preview e reordenação disponíveis. Para editar campo-a-campo, crie uma nova Oferta.",
                    style = MaterialTheme.typography.bodySmall,
                )
                null -> {}
            }
        }

        // ===== 3. SEQUÊNCIA (telas do álbum + tempo) =====
        Appear(delayMillis = 150) { SequenciaCard(vm, editScope, snackbar) }

        // ===== 4. PAINEL (sincronizar / limpar) =====
        Appear(delayMillis = 200) {
            PainelCard(
                temPainel = temPainel,
                busy = panelBusy,
                onSincronizar = { sincronizar() },
                onLimpar = { confirmClear = true },
            )
        }

        // ===== 5. ÁLBUM (salvar / abrir / enviar) =====
        Appear(delayMillis = 250) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    Button(
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); doSave(); nav.goToSend(vm.nome) },
                        shape = ButtonShape,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text("Salvar e enviar") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { exportLauncher.launch("${vm.nome.ifBlank { "Painel" }}.alb") }, shape = ButtonShape, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Upload, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Exportar")
                        }
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }, shape = ButtonShape, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Download, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Importar")
                        }
                    }
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
        }

        if (msg.isNotBlank()) {
            Appear { Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
        }
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

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = { Icon(Icons.Filled.DeleteSweep, null) },
            title = { Text("Limpar todo o painel?") },
            text = { Text("Isso apaga todas as telas gravadas no painel e deixa o display em branco. Não afeta seus álbuns salvos no celular.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; limpar() }) { Text("Limpar") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } },
        )
    }
}

/** Cartão da sequência: telas do álbum, reordenar, excluir a selecionada, tempo. */
@Composable
private fun SequenciaCard(
    vm: EditorViewModel,
    editScope: kotlinx.coroutines.CoroutineScope,
    snackbar: androidx.compose.material3.SnackbarHostState,
) {
    val cur = vm.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Sequência · ${vm.frames.size} ${if (vm.frames.size == 1) "tela" else "telas"}")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                vm.frames.forEachIndexed { i, d ->
                    FilterChip(selected = i == vm.sel, onClick = { vm.selected = i }, label = { Text("${i + 1} · ${d.label()}") })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { vm.addMsg() }, shape = ButtonShape) { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Mensagem") }
                OutlinedButton(onClick = { vm.addOfe() }, shape = ButtonShape) { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Oferta") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.moveUp() }) { Icon(Icons.Filled.KeyboardArrowUp, "Subir") }
                IconButton(onClick = { vm.moveDown() }) { Icon(Icons.Filled.KeyboardArrowDown, "Descer") }
                IconButton(onClick = { vm.duplicate() }) { Icon(Icons.Filled.ContentCopy, "Duplicar") }
                Spacer(Modifier.weight(1f))
                IconButton(enabled = vm.frames.size > 1, onClick = {
                    vm.delete()
                    editScope.launch {
                        val r = snackbar.showSnackbar("Tela excluída", "Desfazer", withDismissAction = true)
                        if (r == SnackbarResult.ActionPerformed) vm.undoDelete()
                    }
                }) { Icon(Icons.Filled.Delete, "Excluir a tela selecionada", tint = MaterialTheme.colorScheme.error) }
            }
            if (cur != null && cur !is FrameDraft.Raw) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SectionLabel("Tempo desta tela")
                TempoSelector(cur.durationIndex) { vm.setDuration(it) }
            }
        }
    }
}

private val AccentGreen = androidx.compose.ui.graphics.Color(0xFF34D399)

/** Ações que falam com o painel físico: sincronizar a sequência e limpar tudo. */
@Composable
private fun PainelCard(
    temPainel: Boolean,
    busy: Boolean,
    onSincronizar: () -> Unit,
    onLimpar: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                CardHeader(Icons.Filled.Sync, AccentGreen, "Painel")
                if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            }
            Text(
                if (temPainel) "Traga o que já está gravado no painel para editar, ou apague tudo."
                else "Conecte-se a um painel (aba Painéis ou USB) para usar estas ações.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onSincronizar, enabled = temPainel && !busy, shape = ButtonShape, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Sync, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Sincronizar")
                }
                OutlinedButton(
                    onClick = onLimpar,
                    enabled = temPainel && !busy,
                    shape = ButtonShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f),
                ) { Icon(Icons.Filled.DeleteSweep, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Limpar") }
            }
        }
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
    var maisCampos by remember { mutableStateOf(s.medida.isNotBlank() || s.auxiliar.isNotBlank() || s.rodape.isNotBlank()) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // interruptores agrupados
        Card {
            Column {
                ToggleRow("Meia tela", draft.halfScreen) { onChange(draft.copy(halfScreen = it)) }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                ToggleRow("Habilitar quadro", s.enabled) { set(s.copy(enabled = it)) }
            }
        }

        // === TEXTOS (cabeçalho/título/subtítulo) ===
        Card {
            Column(Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Textos")
                // cabeçalho OU (título + subtítulo) — excludentes, igual ao painel
                androidx.compose.animation.AnimatedVisibility(!s.subtituloAtivo) {
                    OutlinedTextField(s.cabecalho, { set(s.copy(cabecalho = it)) }, label = { Text("Cabeçalho") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(s.titulo, { set(s.copy(titulo = it)) }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                InlineToggle("Usar subtítulo", s.subtituloAtivo) { set(s.copy(subtituloAtivo = it)) }
                androidx.compose.animation.AnimatedVisibility(s.subtituloAtivo) {
                    OutlinedTextField(s.subtitulo, { set(s.copy(subtitulo = it)) }, label = { Text("Subtítulo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                Text(
                    if (s.subtituloAtivo) "Com subtítulo, o painel mostra Título + Subtítulo — o cabeçalho fica oculto."
                    else "Ative para trocar o cabeçalho por um subtítulo abaixo do título.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // === PREÇO (herói do formulário) ===
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        size = 22,
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

        // === MAIS CAMPOS (medida / auxiliar / rodapé) — colapsável ===
        Card {
            Column(Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.fillMaxWidth().clickable { maisCampos = !maisCampos },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SectionLabel("Complementos")
                    val rot by animateFloatAsState(if (maisCampos) 180f else 0f, label = "expand")
                    Icon(Icons.Filled.KeyboardArrowDown, if (maisCampos) "Recolher" else "Expandir", Modifier.graphicsLayer { rotationZ = rot })
                }
                androidx.compose.animation.AnimatedVisibility(maisCampos) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(s.medida, { set(s.copy(medida = it)) }, label = { Text("Medida") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(s.auxiliar, { set(s.copy(auxiliar = it)) }, label = { Text("Auxiliar") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        OutlinedTextField(s.rodape, { set(s.copy(rodape = it)) }, label = { Text("Rodapé") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

/** Interruptor inline (rótulo + switch) para usar DENTRO de um cartão já com padding. */
@Composable
private fun InlineToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked, onChange)
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
