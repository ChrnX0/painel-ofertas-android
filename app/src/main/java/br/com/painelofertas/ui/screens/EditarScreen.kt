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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.painelofertas.data.PanelStatus
import br.com.painelofertas.editor.DraftCodec
import br.com.painelofertas.editor.FrameDraft
import br.com.painelofertas.editor.LineDraft
import br.com.painelofertas.net.Encriptor
import br.com.painelofertas.net.PanelLink
import br.com.painelofertas.net.UdpLink
import br.com.painelofertas.transfer.TransferProgress
import br.com.painelofertas.protocol.Album
import br.com.painelofertas.protocol.DurationTable
import br.com.painelofertas.protocol.PanelFrame
import br.com.painelofertas.protocol.PanelFont
import br.com.painelofertas.render.AutoLayout
import br.com.painelofertas.render.OfertaSpec
import br.com.painelofertas.render.PanelRenderer
import br.com.painelofertas.ui.components.Accent
import br.com.painelofertas.ui.components.AccentOutlinedButton
import br.com.painelofertas.ui.components.Appear
import br.com.painelofertas.ui.components.ButtonShape
import br.com.painelofertas.ui.components.CardHeader
import br.com.painelofertas.ui.components.LedBezel
import br.com.painelofertas.ui.components.OnAccent
import br.com.painelofertas.ui.components.OnboardingCard
import br.com.painelofertas.ui.components.accentCardColors
import br.com.painelofertas.ui.components.MonoText
import br.com.painelofertas.ui.components.PanelPreview
import br.com.painelofertas.ui.components.SectionLabel
import br.com.painelofertas.ui.components.SegChoice
import br.com.painelofertas.ui.LocalSnackbar
import br.com.painelofertas.ui.rememberContainer
import br.com.painelofertas.ui.theme.ledColorAt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
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
    val historico by container.albums.history.collectAsState()
    val panels by container.panels.panels.collectAsState()
    val usbConnected by container.usb.connected.collectAsState()
    var confirmOverwrite by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmDeleteAlbum by remember { mutableStateOf<String?>(null) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(!container.settings.onboardingDone) }
    var panelBusy by remember { mutableStateOf(false) }
    val ledIdx by container.settings.ledColor.collectAsState()

    val cur = vm.current

    fun doSave() {
        container.albums.save(vm.toAlbum(fonts))
        msg = "Álbum \"${vm.nome}\" salvo (${vm.frames.size} telas)."
    }

    // Alvo das ações de painel (sincronizar / limpar): prefere um painel online,
    // mas aceita QUALQUER painel conhecido (o status "esfria" pra instável em ~15s,
    // então exigir ONLINE travava o botão); se não houver, usa o USB conectado.
    val targetPanel = panels.firstOrNull { it.status == PanelStatus.ONLINE } ?: panels.firstOrNull()
    fun viaUsb() = targetPanel == null && usbConnected
    fun panelLink(): PanelLink? = when {
        targetPanel != null -> UdpLink(targetPanel.ip, container.udp)
        usbConnected -> container.usb.link.value
        else -> null
    }
    val temPainel = targetPanel != null || usbConnected

    // "Cabe no painel?" — avisa ANTES de publicar (null = desconhecido).
    val cabeNoPainel: Boolean? = remember(vm.frames.size, vm.frames.toList(), targetPanel?.freeMemory) {
        val livre = targetPanel?.freeMemory ?: 0
        if (livre <= 0) null else runCatching { vm.toAlbum(fonts).compile().consumo < livre }.getOrNull()
    }

    fun sincronizar() {
        val link = panelLink() ?: run { msg = "Nenhum painel conectado. Abra a aba Painéis para localizar."; return }
        panelBusy = true; msg = "Lendo o painel…"
        val usb = viaUsb()
        container.connection.transferStarted(usb)
        editScope.launch {
            val album = runCatching { container.downloadAlbum(link) }.getOrNull()
            container.connection.transferEnded(usb, album != null)
            val pid = targetPanel?.id ?: "usb"
            val crc = targetPanel?.crcPanel ?: 0
            if (album == null) msg = "Não consegui ler o painel (verifique a conexão)."
            else if (album.frames.isEmpty()) { vm.setLive(album, crc, pid); msg = "O painel está vazio." }
            else {
                vm.setLive(album, crc, pid)
                msg = "Painel lido: ${album.frames.size} tela(s). Arraste a prévia para comparar Editando ↔ No painel."
            }
            panelBusy = false
        }
    }

    /**
     * PUBLICAR: a ação principal do app numa só. Salva o álbum e envia ao painel,
     * com retorno na própria barra. Mata a cerimônia de "salvar → trocar de tela →
     * escolher álbum → escolher painel → enviar".
     */
    fun publicar() {
        val link = panelLink() ?: run { vm.pubError("Nenhum painel conectado."); return }
        val album = vm.toAlbum(fonts)
        container.albums.save(album)          // publica sempre a partir do que está na tela
        val usb = viaUsb()
        vm.pubStart()
        container.connection.transferStarted(usb)
        editScope.launch {
            val r = album.compile()
            val codigo =
                if (container.settings.useTxPassword)
                    Encriptor.code(container.settings.txPassword, System.currentTimeMillis().toString())
                else IntArray(10)
            val ok = runCatching {
                container.transfer(link).upload(r.bytes, codigo, album.brilho) { p ->
                    when (p) {
                        is TransferProgress.Uploading -> vm.pubProgress(p.sent, p.total)
                        else -> {}
                    }
                }
            }.getOrDefault(false)
            container.connection.transferEnded(usb, ok)
            if (ok) {
                targetPanel?.let { container.panels.setExpectedCrc(it.ip, r.crc) }
                vm.setLive(album, targetPanel?.crcPanel ?: 0, targetPanel?.id ?: "usb")
                // guarda no histórico para repetir depois com um toque
                val stamp = SimpleDateFormat("dd-MM HH'h'mm", Locale.getDefault()).format(Date())
                container.albums.pushHistory(album, "${album.name} · $stamp")
                vm.pubDone("✓ No painel agora")
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } else {
                vm.pubError("Não consegui enviar. Verifique o painel e tente de novo.")
            }
            delay(3500)
            vm.pubReset()
        }
    }

    fun limpar() {
        val link = panelLink() ?: run { msg = "Nenhum painel conectado."; return }
        panelBusy = true; msg = "Limpando o painel…"
        val usb = viaUsb()
        container.connection.transferStarted(usb)
        editScope.launch {
            val ok = runCatching { container.clearPanel(link, targetPanel?.brightness ?: 100) }.getOrDefault(false)
            container.connection.transferEnded(usb, ok)
            msg = if (ok) "✅ Painel limpo." else "❌ Falha ao limpar o painel."
            panelBusy = false
        }
    }

    // Rascunho automático: restaura o trabalho em andamento na 1ª composição e
    // salva a cada mudança — o lojista não perde o que digitou se o app for morto.
    LaunchedEffect(Unit) {
        if (!vm.draftRestored) {
            container.albums.loadDraftText()?.let { txt -> DraftCodec.decode(txt)?.let { vm.restoreDraft(it) } }
            vm.draftRestored = true
        }
    }
    LaunchedEffect(vm.frames.toList(), vm.nome, vm.portrait) {
        if (vm.draftRestored) {
            delay(600) // debounce: não grava a cada tecla
            runCatching { container.albums.saveDraftText(DraftCodec.encode(vm.nome, vm.portrait, vm.frames.toList())) }
        }
    }

    // Auto-sincroniza a prévia "No painel": baixa sozinho quando há um painel online
    // (ou USB) e o conteúdo mudou (CRC diferente do último). Sem tocar em botão.
    val syncPanel = panels.firstOrNull { it.status == PanelStatus.ONLINE }
    LaunchedEffect(syncPanel?.id, syncPanel?.crcPanel, usbConnected) {
        val link = when {
            syncPanel != null -> UdpLink(syncPanel.ip, container.udp)
            usbConnected -> container.usb.link.value
            else -> null
        } ?: return@LaunchedEffect
        val pid = syncPanel?.id ?: "usb"
        val crc = syncPanel?.crcPanel ?: 0
        val jaTemos = vm.liveAlbum != null && vm.livePanelId == pid && vm.liveCrc == crc
        if (jaTemos) return@LaunchedEffect
        val usb = syncPanel == null && usbConnected
        container.connection.transferStarted(usb)
        val album = runCatching { container.downloadAlbum(link) }.getOrNull()
        container.connection.transferEnded(usb, album != null)
        if (album != null) vm.setLive(album, crc, pid)
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

    // imePadding: a barra Publicar sobe junto com o teclado (não fica escondida).
    Column(Modifier.fillMaxSize().imePadding()) {

        // ===== FAIXA FIXA (não rola): números + prévia + info =====
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionLabel("Editor de telas")
            ScreensBar(vm) { showNameDialog = true }
            if (cur != null) {
                val frameAtual = cur.build(fonts, vm.portrait)
                PreviewPager(frameAtual, cur.halfScreen, vm.portrait, vm.liveAlbum, vm.sel, ledIdx)

                // Aviso de conteúdo cortado: o painel é físico, o que passa some.
                val over = remember(frameAtual, cur.halfScreen, vm.portrait) {
                    AutoLayout.overflow(frameAtual.records, cur.halfScreen, vm.portrait, fonts)
                }
                androidx.compose.animation.AnimatedVisibility(over.houve) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(Accent.Amber.copy(alpha = 0.16f)).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.WarningAmber, null, tint = Accent.Amber, modifier = Modifier.size(18.dp))
                        Text(
                            buildString {
                                append("Vai aparecer cortado no painel: passa ")
                                if (over.larguraExcedida > 0) append("${over.larguraExcedida} px na largura")
                                if (over.larguraExcedida > 0 && over.alturaExcedida > 0) append(" e ")
                                if (over.alturaExcedida > 0) append("${over.alturaExcedida} px na altura")
                                append(". Encurte o texto ou use uma fonte menor.")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // ===== CONTEÚDO ROLÁVEL =====
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Guia de primeira execução (some para sempre ao dispensar).
            androidx.compose.animation.AnimatedVisibility(showGuide) {
                OnboardingCard(onDismiss = { showGuide = false; container.settings.onboardingDone = true })
            }

            SegChoice(listOf("Horizontal", "Vertical"), if (vm.portrait) 1 else 0, Modifier.fillMaxWidth()) { vm.portrait = it == 1 }

            // Modo de composição: Padrão (oferta pronta) ou Livre (texto solto).
            if (cur != null && cur !is FrameDraft.Raw) {
                val livre = cur is FrameDraft.Msg
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SegChoice(listOf("Padrão", "Livre"), if (livre) 1 else 0, Modifier.fillMaxWidth()) { vm.setMode(it == 1) }
                    Text(
                        if (livre) "Livre: escreva o texto onde quiser — posição e fonte por linha."
                        else "Padrão: campos prontos de oferta (cabeçalho, preço, medida…).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when (val d = cur) {
                is FrameDraft.Msg -> MsgForm(d) { vm.replaceSel(it) }
                is FrameDraft.Ofe -> OfeForm(d) { vm.replaceSel(it) }
                is FrameDraft.Raw -> Text(
                    "Tela salva — prévia e reordenação disponíveis. Para editar campo-a-campo, crie uma nova tela.",
                    style = MaterialTheme.typography.bodySmall,
                )
                null -> {}
            }

            SequenciaCard(vm, editScope, snackbar)

            PainelCard(
                temPainel = temPainel,
                busy = panelBusy,
                onSincronizar = { sincronizar() },
                onLimpar = { confirmClear = true },
            )

            // ===== ÁLBUM =====
            Card(Modifier.fillMaxWidth(), colors = accentCardColors(Accent.Blue)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        SectionLabel("Álbum")
                        TextButton(onClick = { vm.newAlbum(); msg = "Novo álbum em branco." }) {
                            Icon(Icons.Filled.Add, null, Modifier.size(16.dp)); Spacer(Modifier.size(4.dp)); Text("Novo álbum")
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(vm.nome, { vm.nome = it }, label = { Text("Nome do álbum") }, singleLine = true, modifier = Modifier.weight(1f))
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
                        AccentOutlinedButton(onClick = { exportLauncher.launch("${vm.nome.ifBlank { "Painel" }}.alb") }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Upload, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Exportar")
                        }
                        AccentOutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Download, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Importar")
                        }
                    }
                    // Histórico: repetir uma publicação anterior com um toque.
                    if (historico.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.History, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Publicados recentemente — toque para repetir:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            historico.forEach { h ->
                                AssistChip(
                                    onClick = { container.albums.loadHistory(h)?.let { vm.load(it); msg = "Recuperado: $h" } },
                                    label = { Text(h, maxLines = 1) },
                                )
                            }
                        }
                    }

                    if (albuns.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text("Álbuns salvos (toque para abrir, ✕ para excluir):", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            albuns.forEach { a ->
                                InputChip(
                                    selected = vm.nome == a,
                                    onClick = { container.albums.load(a)?.let { vm.load(it); msg = "Aberto \"${it.name}\"." } },
                                    label = { Text(a) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Filled.Close,
                                            "Excluir álbum \"$a\"",
                                            Modifier.size(18.dp).clickable { confirmDeleteAlbum = a },
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (msg.isNotBlank()) {
                Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }

        // ===== BARRA DE PUBLICAÇÃO (fixa) — a ação principal, sempre ao alcance =====
        PublishBar(
            destino = targetPanel?.name ?: if (usbConnected) "Painel por USB" else null,
            cabe = cabeNoPainel,
            state = vm.pubState,
            progress = vm.pubProgress,
            message = vm.pubMessage,
            onPublicar = { publicar() },
        )
    }

    if (showNameDialog) {
        NovaTelaDialog(sugestao = "Tela ${vm.frames.size + 1}", onDismiss = { showNameDialog = false }) { nome, tipoMsg ->
            if (tipoMsg) vm.addMsg(nome) else vm.addOfe(nome)
            showNameDialog = false
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

    confirmDeleteAlbum?.let { alb ->
        AlertDialog(
            onDismissRequest = { confirmDeleteAlbum = null },
            icon = { Icon(Icons.Filled.Delete, null) },
            title = { Text("Excluir álbum?") },
            text = { Text("Remover \"$alb\" deste aparelho. Não afeta o que já está gravado no painel.") },
            confirmButton = {
                TextButton(onClick = { container.albums.delete(alb); msg = "Álbum \"$alb\" excluído."; confirmDeleteAlbum = null }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAlbum = null }) { Text("Cancelar") } },
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
    Card(Modifier.fillMaxWidth(), colors = accentCardColors(Accent.Teal)) {
        Column(Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Sequência · ${vm.frames.size} ${if (vm.frames.size == 1) "tela" else "telas"}")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                vm.frames.forEachIndexed { i, d ->
                    FilterChip(selected = i == vm.sel, onClick = { vm.selected = i }, label = { Text("${i + 1} · ${d.display()}") })
                }
            }
            if (cur != null) {
                OutlinedTextField(
                    cur.name, { vm.renameFrame(vm.sel, it) },
                    label = { Text("Nome desta tela") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
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

/** Ações que falam com o painel físico: sincronizar a sequência e limpar tudo. */
@Composable
private fun PainelCard(
    temPainel: Boolean,
    busy: Boolean,
    onSincronizar: () -> Unit,
    onLimpar: () -> Unit,
) {
    Card(Modifier.fillMaxWidth(), colors = accentCardColors(Accent.Green)) {
        Column(Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                CardHeader(Icons.Filled.Sync, Accent.Green, "Painel")
                if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            }
            Text(
                if (temPainel) "Traga o que já está gravado no painel para editar, ou apague tudo."
                else "Conecte-se a um painel (aba Painéis ou USB) para usar estas ações.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AccentOutlinedButton(onClick = onSincronizar, enabled = temPainel && !busy, accent = Accent.Green, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Sync, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Sincronizar com o painel")
                }
                AccentOutlinedButton(onClick = onLimpar, enabled = temPainel && !busy, accent = Accent.Rose, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.DeleteSweep, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Limpar painel")
                }
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

/**
 * Barra fixa de publicação — a ação principal do app, sempre visível no editor.
 * Salva + envia numa tacada, mostra para onde vai, avisa se não cabe na memória,
 * e dá o retorno ali mesmo (enviando / no painel / falhou).
 */
@Composable
private fun PublishBar(
    destino: String?,
    cabe: Boolean?,
    state: EditorViewModel.PubState,
    progress: Float?,
    message: String,
    onPublicar: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.surface, tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).animateContentSize()) {
            // barra fina de progresso durante o envio
            androidx.compose.animation.AnimatedVisibility(state == EditorViewModel.PubState.WORKING) {
                LinearProgressIndicator(
                    progress = { progress ?: 0f },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    when (state) {
                        EditorViewModel.PubState.DONE ->
                            Text(message, style = MaterialTheme.typography.titleSmall, color = Accent.Green)
                        EditorViewModel.PubState.ERROR ->
                            Text(message, style = MaterialTheme.typography.bodySmall, color = Accent.Rose)
                        EditorViewModel.PubState.WORKING ->
                            Text(message, style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)
                        EditorViewModel.PubState.IDLE -> {
                            if (destino != null) {
                                MonoText("PARA", size = 9)
                                Text(destino, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                if (cabe == false) {
                                    Text("⚠ não cabe na memória do painel", style = MaterialTheme.typography.bodySmall, color = Accent.Rose)
                                }
                            } else {
                                Text("Nenhum painel encontrado", style = MaterialTheme.typography.titleSmall)
                                Text("Ligue o painel na mesma rede Wi-Fi.", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                            }
                        }
                    }
                }

                Button(
                    onClick = onPublicar,
                    enabled = destino != null && state != EditorViewModel.PubState.WORKING,
                    shape = ButtonShape,
                    modifier = Modifier.height(50.dp).semantics { contentDescription = "Publicar no painel" },
                ) {
                    if (state == EditorViewModel.PubState.WORKING) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = cs.onPrimary)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(19.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Publicar", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

/** Fileira de telas numeradas (One UI) + botão "+". Desliza na horizontal. */
@Composable
private fun ScreensBar(vm: EditorViewModel, onAdd: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        vm.frames.forEachIndexed { i, d ->
            val selected = i == vm.sel
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(15.dp))
                    .background(if (selected) cs.primary else cs.surfaceContainerHigh)
                    .clickable { vm.selected = i }
                    .semantics { contentDescription = "Tela ${i + 1}: ${d.display()}" + if (selected) ", selecionada" else "" },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${i + 1}",
                    color = if (selected) OnAccent else cs.onSurface,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(cs.surfaceContainerHigh).clickable { onAdd() },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Add, "Adicionar nova tela", tint = cs.primary) }
    }
}

/** Prévia deslizável: página 0 = a tela sendo editada; página 1 = o que está no painel. */
@Composable
private fun PreviewPager(editing: PanelFrame, half: Boolean, portrait: Boolean, live: Album?, liveIndex: Int, ledIdx: Int) {
    val pager = rememberPagerState(pageCount = { 2 })
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalPager(state = pager, pageSpacing = 12.dp, modifier = Modifier.fillMaxWidth()) { page ->
            if (page == 0) {
                PreviewCard(editing, half, portrait, badge = "EDITANDO", boardHeight = 126.dp)
            } else {
                val liveFrames = live?.frames.orEmpty()
                if (liveFrames.isNotEmpty()) {
                    val f = liveFrames[liveIndex.coerceIn(0, liveFrames.lastIndex)]
                    PreviewCard(f, f.halfScreen, portrait, badge = "NO PAINEL", boardHeight = 126.dp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LedBezel(Modifier.fillMaxWidth(), boardHeight = 126.dp) {
                            Text(
                                "O conteúdo do painel aparece aqui\nsozinho quando um painel é encontrado.",
                                color = Color(0xFF6A7480),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                        MonoText("NO PAINEL · procurando conteúdo…", size = 10)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(2) { i ->
                Box(
                    Modifier.padding(horizontal = 3.dp).size(7.dp).clip(CircleShape)
                        .background(if (pager.currentPage == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
    }
}

/** Diálogo do "+": nomear a nova tela e escolher o tipo. */
@Composable
private fun NovaTelaDialog(sugestao: String, onDismiss: () -> Unit, onCreate: (String, Boolean) -> Unit) {
    var nome by remember { mutableStateOf("") }
    var tipoMsg by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Add, null) },
        title = { Text("Nova tela") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(nome, { nome = it }, label = { Text("Nome (ex.: Picanha)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                SegChoice(listOf("Oferta", "Mensagem"), if (tipoMsg) 1 else 0, Modifier.fillMaxWidth()) { tipoMsg = it == 1 }
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(nome.ifBlank { sugestao }, tipoMsg) }) { Text("Criar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun PreviewCard(
    frame: PanelFrame,
    halfScreen: Boolean,
    portrait: Boolean,
    badge: String = "PRÉVIA AO VIVO",
    boardHeight: Dp = 138.dp,
) {
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
        LedBezel(Modifier.fillMaxWidth(), boardHeight = boardHeight) {
            PanelPreview(
                bmp,
                Modifier.fillMaxSize().padding(10.dp).graphicsLayer { alpha = pulse },
                litColor = led,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(led))
            MonoText(
                "$badge · ${if (halfScreen) "MEIA" else "CHEIA"}" +
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

        // Auto-justificar: o app centraliza e distribui sozinho no display.
        Card(colors = accentCardColors(Accent.Teal)) {
            Column(Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InlineToggle("Ajustar automaticamente", draft.autoFit) { onChange(draft.copy(autoFit = it)) }
                Text(
                    if (draft.autoFit) "O texto é centralizado e distribuído sozinho, na maior fonte que couber."
                    else "Você escolhe linha, coluna e fonte de cada linha de texto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SectionLabel("Linhas de texto")
        draft.lines.forEachIndexed { i, linha ->
            LineCard(
                linha,
                autoFit = draft.autoFit,
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
private fun LineCard(linha: LineDraft, autoFit: Boolean, onChange: (LineDraft) -> Unit, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(linha.text, { onChange(linha.copy(text = it)) }, label = { Text("Texto") }, singleLine = true, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, "Remover linha") }
            }
            // Com auto-ajuste, posição e fonte são calculadas — não há o que mexer.
            androidx.compose.animation.AnimatedVisibility(!autoFit) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            androidx.compose.animation.AnimatedVisibility(autoFit) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Tamanho máximo:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PanelFont.entries.forEach { f ->
                        FilterChip(linha.font == f.code, { onChange(linha.copy(font = f.code)) }, { Text(f.displayName, style = MaterialTheme.typography.labelSmall) })
                    }
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
        Card(colors = accentCardColors(Accent.Amber)) {
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
                // Medida é a unidade do preço → mora aqui (antes ficava em "Complementos").
                OutlinedTextField(s.medida, { set(s.copy(medida = it)) }, label = { Text("Medida (ex.: O KILO)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }

        // === RODAPÉ (textos auxiliares) ===
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Rodapé")
                OutlinedTextField(s.auxiliar, { set(s.copy(auxiliar = it)) }, label = { Text("Auxiliar") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(s.rodape, { set(s.copy(rodape = it)) }, label = { Text("Rodapé (ex.: OFERTA VÁLIDA HOJE)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
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
