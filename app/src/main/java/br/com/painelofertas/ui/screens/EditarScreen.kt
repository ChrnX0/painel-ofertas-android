package br.com.painelofertas.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
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
import br.com.painelofertas.editor.PriceListParser
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
import br.com.painelofertas.ui.components.DestinoSheet
import br.com.painelofertas.ui.components.LedBezel
import br.com.painelofertas.ui.components.OnAccent
import br.com.painelofertas.ui.components.OnboardingCard
import br.com.painelofertas.ui.components.accentBorder
import br.com.painelofertas.ui.components.accentCardColors
import br.com.painelofertas.ui.components.Motion
import br.com.painelofertas.ui.components.pressBounce
import br.com.painelofertas.ui.components.MonoText
import br.com.painelofertas.ui.components.OptionTile
import br.com.painelofertas.ui.components.PanelShape
import br.com.painelofertas.ui.components.PanelPreview
import br.com.painelofertas.ui.components.SectionLabel
import br.com.painelofertas.ui.components.SoftDivider
import br.com.painelofertas.ui.components.SegChoice
import br.com.painelofertas.ui.LocalSnackbar
import br.com.painelofertas.ui.rememberContainer
import br.com.painelofertas.ui.theme.SquircleShape
import br.com.painelofertas.ui.theme.ledColorAt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    var msg by remember { mutableStateOf("") }
    val albuns by container.albums.names.collectAsState()
    val historico by container.albums.history.collectAsState()
    val panels by container.panels.panels.collectAsState()
    val usbConnected by container.usb.connected.collectAsState()
    var confirmOverwrite by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmDeleteAlbum by remember { mutableStateOf<String?>(null) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(!container.settings.onboardingDone) }
    var tocando by remember { mutableStateOf(false) }

    // Prévia animada: avança pelas telas usando o tempo de cada uma (como o painel).
    LaunchedEffect(tocando, vm.frames.size) {
        if (!tocando || vm.frames.size <= 1) return@LaunchedEffect
        while (tocando) {
            val seg = DurationTable.secondsByIndex.getOrNull(vm.current?.durationIndex ?: 0) ?: 0
            delay(if (seg > 0) seg * 1000L else 3000L)  // 0 = "auto" → 3s
            vm.selected = (vm.sel + 1) % vm.frames.size
        }
    }
    var panelBusy by remember { mutableStateOf(false) }
    val ledIdx by container.settings.ledColor.collectAsState()

    val cur = vm.current

    fun doSave() {
        container.albums.save(vm.toAlbum(fonts))
        msg = "Álbum \"${vm.nome}\" salvo (${vm.frames.size} telas)."
    }

    // ===== DESTINO DA PUBLICAÇÃO =====
    // Escolhido na própria barra Publicar (absorve o que era a aba "Enviar"):
    // um painel, vários, um grupo inteiro ou o USB.
    var destinos by remember { mutableStateOf(setOf<String>()) }
    var usarUsb by remember { mutableStateOf(false) }
    var mostrarDestino by remember { mutableStateOf(false) }
    var usarSenha by remember { mutableStateOf(container.settings.useTxPassword) }
    var senhaTx by remember { mutableStateOf(container.settings.txPassword) }

    // Sem escolha explícita, usa o painel online (ou qualquer conhecido) — o caso
    // comum de uma loja com um painel só continua sendo zero-toque.
    val painelPadrao = panels.firstOrNull { it.status == PanelStatus.ONLINE } ?: panels.firstOrNull()
    val ipsDestino: List<String> = when {
        usarUsb -> emptyList()
        destinos.isNotEmpty() -> destinos.toList()
        painelPadrao != null -> listOf(painelPadrao.ip)
        else -> emptyList()
    }
    // Painel de referência (nome, memória livre, CRC). Só cai no padrão quando não
    // há escolha explícita — senão um IP digitado à mão herdaria o nome de outro.
    val targetPanel =
        if (ipsDestino.isEmpty()) painelPadrao
        else panels.firstOrNull { it.ip == ipsDestino.first() }
    fun viaUsb() = usarUsb && usbConnected
    fun panelLink(): PanelLink? = when {
        viaUsb() -> container.usb.link.value
        targetPanel != null -> UdpLink(targetPanel.ip, container.udp)
        usbConnected -> container.usb.link.value
        else -> null
    }
    val temPainel = ipsDestino.isNotEmpty() || (usbConnected && usarUsb) || targetPanel != null || usbConnected

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
        val usb = viaUsb()
        // Alvos: o USB, ou a lista de IPs escolhida (um, vários ou um grupo).
        val alvos: List<PanelLink?> =
            if (usb) listOf(container.usb.link.value)
            else ipsDestino.map { container.udpLinkByIp(it) }
        if (alvos.isEmpty() || alvos.all { it == null }) { vm.pubError("Nenhum painel conectado."); return }

        val album = vm.toAlbum(fonts)
        container.albums.save(album)          // publica sempre a partir do que está na tela
        vm.pubStart()
        container.connection.transferStarted(usb)
        editScope.launch {
            val r = album.compile()
            var okTotal = 0
            val falhas = mutableListOf<String>()

            alvos.forEachIndexed { idx, link ->
                if (link == null) return@forEachIndexed
                val ip = if (usb) "USB" else ipsDestino[idx]
                val codigo =
                    if (usarSenha) Encriptor.code(senhaTx, System.currentTimeMillis().toString())
                    else IntArray(10)
                val rotulo =
                    if (alvos.size == 1) ""
                    else "Painel ${idx + 1} de ${alvos.size} (${panels.firstOrNull { it.ip == ip }?.name ?: ip})"
                val ok = runCatching {
                    container.transfer(link).upload(r.bytes, codigo, album.brilho) { p ->
                        if (p is TransferProgress.Uploading && p.total > 0) {
                            // Progresso agregado: uma barra só, que nunca volta pra trás.
                            val doPainel = p.sent.toFloat() / p.total
                            vm.pubProgress(((idx + doPainel) * 100).toInt(), alvos.size * 100, rotulo)
                        }
                    }
                }.getOrDefault(false)
                if (ok) { okTotal++; if (!usb) container.panels.setExpectedCrc(ip, r.crc) } else falhas.add(ip)
            }

            val tudoOk = falhas.isEmpty() && okTotal > 0
            container.connection.transferEnded(usb, tudoOk)
            if (okTotal > 0) {
                vm.setLive(album, targetPanel?.crcPanel ?: 0, targetPanel?.id ?: "usb")
                val stamp = SimpleDateFormat("dd-MM HH'h'mm", Locale.getDefault()).format(Date())
                container.albums.pushHistory(album, "${album.name} · $stamp")
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            when {
                tudoOk && alvos.size == 1 -> vm.pubDone("✓ No painel agora")
                tudoOk -> vm.pubDone("✓ No ar em $okTotal painéis")
                okTotal > 0 -> vm.pubError("Enviado para $okTotal; falhou em ${falhas.joinToString()}")
                else -> vm.pubError("Não consegui enviar. Verifique o painel e tente de novo.")
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
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                SectionLabel("Editor de telas")
                // Prévia animada: roda a sequência como o painel vai exibir.
                if (vm.frames.size > 1) {
                    AccentOutlinedButton(
                        onClick = { tocando = !tocando },
                        accent = if (tocando) Accent.Rose else Accent.Teal,
                    ) {
                        Icon(
                            if (tocando) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            if (tocando) "Parar a prévia" else "Ver a sequência rodando",
                            Modifier.size(16.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(if (tocando) "Parar" else "Ver rodando", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
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

            // Entrada em cascata: cada bloco sobe e assenta um pouco depois do
            // anterior. A tela se monta diante do usuário em vez de aparecer pronta.
            Appear(delayMillis = 40) {
                // Um lugar só para "como esta tela é", na ordem em que se pensa:
                // formato do painel → tamanho da tela → o que ela mostra → entra ou não.
                SetupCard(vm, cur)
            }

            Appear(delayMillis = 90) {
                when (val d = cur) {
                    is FrameDraft.Msg -> MsgForm(d) { vm.replaceSel(it) }
                    is FrameDraft.Ofe -> OfeForm(d) { vm.replaceSel(it) }
                    is FrameDraft.Raw -> Text(
                        "Tela salva — prévia e reordenação disponíveis. Para editar campo-a-campo, crie uma nova tela.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    null -> {}
                }
            }

            Appear(delayMillis = 140) { SequenciaCard(vm, editScope, snackbar) }

            Appear(delayMillis = 190) {
                PainelCard(
                    temPainel = temPainel,
                    busy = panelBusy,
                    onSincronizar = { sincronizar() },
                    onLimpar = { confirmClear = true },
                )
            }

            // ===== ÁLBUM =====
            Appear(delayMillis = 240) {
            Card(Modifier.fillMaxWidth(), colors = accentCardColors(Accent.Blue), border = accentBorder(Accent.Blue)) {
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
                    // "Salvar e enviar" saiu: Publicar (barra fixa embaixo) já salva
                    // o álbum antes de mandar. Um caminho só, sem passo extra.
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
            }

            if (msg.isNotBlank()) {
                Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }

        // ===== BARRA DE PUBLICAÇÃO (fixa) — a ação principal, sempre ao alcance =====
        PublishBar(
            destino = when {
                viaUsb() -> "Painel por USB"
                ipsDestino.size > 1 -> "${ipsDestino.size} painéis"
                // IP digitado na mão ainda não está na lista descoberta: mostra o IP.
                ipsDestino.size == 1 -> targetPanel?.name ?: ipsDestino[0]
                usbConnected -> "Painel por USB"
                else -> null
            },
            cabe = cabeNoPainel,
            state = vm.pubState,
            progress = vm.pubProgress,
            message = vm.pubMessage,
            onTrocarDestino = { mostrarDestino = true },
            onPublicar = { publicar() },
        )
    }

    if (mostrarDestino) {
        DestinoSheet(
            paineis = panels,
            grupos = remember(panels) { container.panels.groups() },
            ipsDoGrupo = { container.panels.ipsOfGroup(it) },
            selecionados = destinos,
            usbConectado = usbConnected,
            usarUsb = usarUsb,
            usarSenha = usarSenha,
            senha = senhaTx,
            onSelecionados = { destinos = it; if (it.isNotEmpty()) usarUsb = false },
            onUsarUsb = { usarUsb = it; if (it) destinos = emptySet() },
            onUsarSenha = { usarSenha = it; container.settings.useTxPassword = it },
            onSenha = { senhaTx = it; container.settings.txPassword = it },
            onDismiss = { mostrarDestino = false },
        )
    }

    if (showNameDialog) {
        NovaTelaDialog(
            sugestao = "Tela ${vm.frames.size + 1}",
            onDismiss = { showNameDialog = false },
            onLote = { showNameDialog = false; showBatchDialog = true },
        ) { nome, tipoMsg ->
            if (tipoMsg) vm.addMsg(nome) else vm.addOfe(nome)
            showNameDialog = false
        }
    }

    if (showBatchDialog) {
        LoteDialog(onDismiss = { showBatchDialog = false }) { texto, substituir ->
            val n = vm.addBatch(texto, substituir)
            msg = if (n > 0) "$n tela(s) criada(s) da lista." else "Nada para criar."
            showBatchDialog = false
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
    Card(Modifier.fillMaxWidth(), colors = accentCardColors(Accent.Teal), border = accentBorder(Accent.Teal)) {
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
    Card(Modifier.fillMaxWidth(), colors = accentCardColors(Accent.Green), border = accentBorder(Accent.Green)) {
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
/**
 * "Como esta tela é" — o único lugar onde se decide formato e conteúdo.
 *
 * Antes eram quatro controles em três idiomas diferentes (dois segmentados no topo
 * do formulário e dois interruptores num cartão lá embaixo), misturando o que vale
 * para o painel inteiro com o que vale só para a tela atual. Aqui os quatro viram
 * a mesma coisa — pares de botões com o desenho do resultado — na ordem natural:
 * **o painel** (deitado/em pé) → **esta tela** (tamanho, conteúdo) → **entra ou não**.
 */
@Composable
private fun SetupCard(vm: EditorViewModel, cur: FrameDraft?) {
    val cs = MaterialTheme.colorScheme
    val editavel = cur != null && cur !is FrameDraft.Raw
    val livre = cur is FrameDraft.Msg
    val meia = vm.currentHalf ?: true
    val ligada = vm.currentEnabled ?: true

    Card(
        Modifier.fillMaxWidth(),
        colors = accentCardColors(Accent.Sky, 0.14f),
        border = accentBorder(Accent.Sky),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // --- 1. O painel (vale para todas as telas) ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("O painel · vale para todas as telas", accent = Accent.Sky)
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OptionTile(
                        selected = !vm.portrait, title = "Deitado", hint = "Painel na horizontal",
                        accent = Accent.Sky, modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { vm.portrait = false },
                    ) { PanelShape(1f, tall = false, accent = Accent.Sky, on = !vm.portrait) }
                    OptionTile(
                        selected = vm.portrait, title = "Em pé", hint = "Painel na vertical",
                        accent = Accent.Sky, modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { vm.portrait = true },
                    ) { PanelShape(1f, tall = true, accent = Accent.Sky, on = vm.portrait) }
                }
            }

            if (editavel) {
                SoftDivider(accent = Accent.Teal)

                // --- 2. Tamanho desta tela ---
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Esta tela ocupa", accent = Accent.Teal)
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OptionTile(
                            selected = !meia, title = "Painel inteiro", hint = "Uma tela por vez",
                            accent = Accent.Teal, modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { vm.setHalf(false) },
                        ) { PanelShape(1f, vm.portrait, Accent.Teal, on = !meia) }
                        OptionTile(
                            selected = meia, title = "Metade", hint = "Duas telas lado a lado",
                            accent = Accent.Teal, modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { vm.setHalf(true) },
                        ) { PanelShape(0.5f, vm.portrait, Accent.Teal, on = meia) }
                    }
                }

                SoftDivider(accent = Accent.Amber)

                // --- 3. Conteúdo ---
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Esta tela mostra", accent = Accent.Amber)
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OptionTile(
                            selected = !livre, title = "Oferta", hint = "Preço grande, campos prontos",
                            accent = Accent.Amber, modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { vm.setMode(false) },
                        ) { Icon(Icons.Filled.LocalOffer, null, Modifier.size(24.dp), tint = if (!livre) Accent.Amber else cs.outline) }
                        OptionTile(
                            selected = livre, title = "Texto livre", hint = "Escreva o que quiser",
                            accent = Accent.Amber, modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { vm.setMode(true) },
                        ) { Icon(Icons.Filled.Notes, null, Modifier.size(24.dp), tint = if (livre) Accent.Amber else cs.outline) }
                    }
                }

                SoftDivider(accent = Accent.Green)

                // --- 4. O liga/desliga de verdade ---
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Mostrar no painel", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (ligada) "Esta tela entra na rotação."
                            else "Desligada: fica salva, mas o painel pula ela.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (ligada) cs.onSurfaceVariant else Accent.Amber,
                        )
                    }
                    Switch(ligada, { vm.setEnabled(it) })
                }
            }
        }
    }
}

@Composable
private fun PublishBar(
    destino: String?,
    cabe: Boolean?,
    state: EditorViewModel.PubState,
    progress: Float?,
    message: String,
    onTrocarDestino: () -> Unit,
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
                Column(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = state != EditorViewModel.PubState.WORKING) { onTrocarDestino() }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    when (state) {
                        EditorViewModel.PubState.DONE ->
                            Text(message, style = MaterialTheme.typography.titleSmall, color = Accent.Green)
                        EditorViewModel.PubState.ERROR ->
                            Text(message, style = MaterialTheme.typography.bodySmall, color = Accent.Rose)
                        EditorViewModel.PubState.WORKING ->
                            Text(message, style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)
                        EditorViewModel.PubState.IDLE -> {
                            if (destino != null) {
                                MonoText("PARA · TOQUE PARA TROCAR", size = 9)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(destino, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                    Icon(Icons.Filled.ExpandMore, null, Modifier.size(16.dp), tint = cs.onSurfaceVariant)
                                }
                                if (cabe == false) {
                                    Text("⚠ não cabe na memória do painel", style = MaterialTheme.typography.bodySmall, color = Accent.Rose)
                                }
                            } else {
                                Text("Nenhum painel encontrado", style = MaterialTheme.typography.titleSmall)
                                // Saída pelo toque: dentro da folha dá pra digitar o IP na mão.
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "Ligue-o na mesma rede Wi-Fi, ou toque para digitar o IP",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cs.primary,
                                    )
                                    Icon(Icons.Filled.ExpandMore, null, Modifier.size(14.dp), tint = cs.primary)
                                }
                            }
                        }
                    }
                }

                // O botão principal **respira** enquanto está pronto: um pulso lento,
                // quase imperceptível, que diz "estou vivo, é aqui". Some no instante
                // em que o envio começa — respirar durante o trabalho seria ruído.
                val pronto = destino != null && state == EditorViewModel.PubState.IDLE
                val respiro = rememberInfiniteTransition(label = "publicar")
                val pulso by respiro.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.035f,
                    animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "pulso",
                )
                val escalaOk by animateFloatAsState(
                    if (state == EditorViewModel.PubState.DONE) 1.06f else 1f,
                    Motion.springy(), label = "publicarOk",
                )
                val press = remember { MutableInteractionSource() }
                Button(
                    onClick = onPublicar,
                    enabled = destino != null && state != EditorViewModel.PubState.WORKING,
                    shape = ButtonShape,
                    interactionSource = press,
                    modifier = Modifier
                        .graphicsLayer {
                            val s = (if (pronto) pulso else 1f) * escalaOk
                            scaleX = s; scaleY = s
                        }
                        .pressBounce(press, scaleDown = 0.94f)
                        .height(50.dp)
                        .semantics { contentDescription = "Publicar no painel" },
                ) {
                    AnimatedContent(
                        targetState = state,
                        transitionSpec = {
                            (fadeIn(Motion.gentle()) + scaleIn(Motion.bouncy(), initialScale = 0.7f)) togetherWith
                                (fadeOut(tween(120)) + scaleOut(Motion.gentle(), targetScale = 0.7f))
                        },
                        label = "publicarConteudo",
                    ) { st ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when (st) {
                                EditorViewModel.PubState.WORKING ->
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = cs.onPrimary)
                                EditorViewModel.PubState.DONE -> {
                                    Icon(Icons.Filled.Check, null, Modifier.size(20.dp))
                                    Spacer(Modifier.size(8.dp)); Text("No ar", style = MaterialTheme.typography.titleSmall)
                                }
                                else -> {
                                    Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(19.dp))
                                    Spacer(Modifier.size(8.dp)); Text("Publicar", style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fileira de telas numeradas + botão "+". A selecionada **se alarga** e cresce
 * com mola, em vez de só trocar de cor: o dedo sente qual está ativa mesmo de
 * relance, e a mudança tem direção — parece que a pastilha se move, não que duas
 * piscaram. Desliza na horizontal.
 */
@Composable
private fun ScreensBar(vm: EditorViewModel, onAdd: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val forma = SquircleShape(17.dp)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        vm.frames.forEachIndexed { i, d ->
            val selected = i == vm.sel
            val press = remember { MutableInteractionSource() }
            val largura by animateDpAsState(if (selected) 62.dp else 48.dp, Motion.bouncy(), label = "tabW")
            val cor by animateColorAsState(
                if (selected) cs.primary else cs.surfaceContainerHigh,
                Motion.gentle(), label = "tabColor",
            )
            Box(
                Modifier.width(largura).height(48.dp)
                    .pressBounce(press)
                    .clip(forma)
                    .background(cor)
                    .clickable(interactionSource = press, indication = null) { vm.selected = i }
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
        val pressAdd = remember { MutableInteractionSource() }
        Box(
            Modifier.size(48.dp)
                .pressBounce(pressAdd)
                .clip(forma)
                .background(cs.surfaceContainerHigh)
                .clickable(interactionSource = pressAdd, indication = null) { onAdd() },
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

/** Diálogo do "+": nomear a nova tela, escolher o tipo — ou criar várias de uma vez. */
@Composable
private fun NovaTelaDialog(
    sugestao: String,
    onDismiss: () -> Unit,
    onLote: () -> Unit,
    onCreate: (String, Boolean) -> Unit,
) {
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AccentOutlinedButton(onClick = onLote, accent = Accent.Teal, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.PlaylistAdd, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp))
                    Text("Criar várias de uma lista")
                }
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(nome.ifBlank { sugestao }, tipoMsg) }) { Text("Criar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/**
 * **Criação em lote**: cole a lista de ofertas — uma por linha — e receba o álbum
 * pronto. Cada linha vira uma tela diagramada com o preço em destaque.
 */
@Composable
private fun LoteDialog(onDismiss: () -> Unit, onCriar: (String, Boolean) -> Unit) {
    var texto by remember { mutableStateOf("") }
    var substituir by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf("") }
    val linhas = texto.split('\n').count { it.isNotBlank() }

    // Importar planilha de preços (CSV/TXT exportado do Excel, Sheets ou PDV).
    val ctx = LocalContext.current
    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val bruto = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
                val ofertas = PriceListParser.parse(bruto)
                if (ofertas.isEmpty()) aviso = "Não encontrei ofertas nesse arquivo."
                else { texto = ofertas.joinToString("\n"); aviso = "${ofertas.size} oferta(s) lida(s) da planilha." }
            }.onFailure { aviso = "Não consegui ler o arquivo." }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.PlaylistAdd, null) },
        title = { Text("Criar várias telas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Uma oferta por linha. O app separa o preço e monta cada tela.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AccentOutlinedButton(
                    onClick = { importar.launch(arrayOf("*/*")) },
                    accent = Accent.Blue,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.TableChart, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp))
                    Text("Importar planilha de preços")
                }
                if (aviso.isNotBlank()) {
                    MonoText(aviso, size = 11, color = Accent.Teal)
                }
                OutlinedTextField(
                    texto, { texto = it },
                    label = { Text("Lista de ofertas") },
                    placeholder = { Text("PICANHA 9,90 O KILO\nALCATRA 7,90 O KILO\nFRALDINHA 6,50 O KILO") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
                InlineToggle("Substituir as telas atuais", substituir) { substituir = it }
                if (linhas > 0) {
                    MonoText("$linhas tela(s) serão criadas", size = 11, color = Accent.Teal)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCriar(texto, substituir) }, enabled = linhas > 0) { Text("Criar $linhas") }
        },
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
    // Tamanho e liga/desliga vivem no SetupCard, no topo — aqui fica só o conteúdo.
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionLabel("Borda")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Sem", "Segmentada", "Contínua").forEachIndexed { i, nome ->
                    FilterChip(draft.border == i, { onChange(draft.copy(border = i)) }, { Text(nome, style = MaterialTheme.typography.labelSmall) })
                }
            }
        }

        // Auto-ajuste: o app quebra, dimensiona e centraliza o texto sozinho.
        Card(colors = accentCardColors(Accent.Teal), border = accentBorder(Accent.Teal)) {
            Column(Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InlineToggle("Ajustar o texto à tela", draft.autoFit) { onChange(draft.copy(autoFit = it)) }
                Text(
                    if (draft.autoFit) "Escreva à vontade: o app quebra as linhas, escolhe a maior fonte que couber e centraliza."
                    else "Você escolhe linha, coluna e fonte de cada linha de texto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (draft.autoFit) {
            AutoTextCard(draft, onChange)
        } else {
            SectionLabel("Linhas de texto")
            draft.lines.forEachIndexed { i, linha ->
                LineCard(
                    linha,
                    autoFit = false,
                    onChange = { nl -> onChange(draft.copy(lines = draft.lines.toMutableList().also { it[i] = nl })) },
                    onRemove = { onChange(draft.copy(lines = draft.lines.toMutableList().also { if (it.size > 1) it.removeAt(i) })) },
                )
            }
            OutlinedButton(onClick = { onChange(draft.copy(lines = draft.lines + LineDraft())) }, shape = ButtonShape) {
                Icon(Icons.Filled.Add, null); Text("Linha")
            }
        }
    }
}

/**
 * Modo automático: um único campo de texto corrido. O app quebra em linhas,
 * escolhe a maior fonte em que tudo cabe e centraliza — e mostra ao vivo o que
 * decidiu ("3 linhas · fonte Terceira"), para o lojista entender o resultado.
 */
@Composable
private fun AutoTextCard(draft: FrameDraft.Msg, onChange: (FrameDraft.Msg) -> Unit) {
    val fonts = rememberContainer().fonts
    val texto = draft.textoParaAjuste()

    val fit = remember(texto, draft.halfScreen, draft.maxFont, draft.align, draft.smart) {
        if (draft.smart) AutoLayout.smartFit(texto, draft.halfScreen, false, fonts, draft.maxFont, draft.align)
        else AutoLayout.fitParagraph(texto, draft.halfScreen, false, fonts, draft.maxFont, draft.align)
    }
    val destaque = remember(texto, draft.smart) {
        if (draft.smart) AutoLayout.smartSplit(texto).firstOrNull { it.hero }?.text else null
    }

    Card {
        Column(Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Texto")
            OutlinedTextField(
                value = texto,
                onValueChange = { onChange(draft.copy(freeText = it)) },
                label = { Text("Escreva o que aparece no painel") },
                placeholder = { Text("Ex.: PICANHA 9,90 O KILO") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            // Composição inteligente: reconhece o preço e o coloca em destaque.
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                InlineToggle("Destacar o preço", draft.smart) { onChange(draft.copy(smart = it)) }
                Text(
                    when {
                        !draft.smart -> "Todas as linhas com o mesmo tamanho."
                        destaque != null -> "Reconheci \"$destaque\" como preço — vai em letra grande."
                        else -> "Escreva um preço (ex.: 9,90) e ele ganha destaque automaticamente."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (destaque != null) Accent.Teal else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // O que o app decidiu — transparência sobre o ajuste automático.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    if (fit.fits) Icons.Filled.AutoAwesome else Icons.Filled.WarningAmber,
                    null,
                    Modifier.size(16.dp),
                    tint = if (fit.fits) Accent.Teal else Accent.Amber,
                )
                MonoText(
                    if (fit.lines.isEmpty()) "escreva algo acima"
                    else "${fit.lines.size} linha(s)" +
                        (if (destaque != null) " · destaque ${PanelFont.of(fit.fontCode).displayName}" else " · fonte ${PanelFont.of(fit.fontCode).displayName}") +
                        if (fit.fits) "" else " · não cabe nem no menor tamanho",
                    size = 11,
                    color = if (fit.fits) MaterialTheme.colorScheme.onSurfaceVariant else Accent.Amber,
                )
            }

            SectionLabel("Alinhamento")
            SegChoice(
                listOf("Esquerda", "Centro", "Direita"),
                when (draft.align) {
                    AutoLayout.Align.LEFT -> 0
                    AutoLayout.Align.CENTER -> 1
                    AutoLayout.Align.RIGHT -> 2
                },
                Modifier.fillMaxWidth(),
            ) {
                onChange(draft.copy(align = when (it) {
                    0 -> AutoLayout.Align.LEFT
                    2 -> AutoLayout.Align.RIGHT
                    else -> AutoLayout.Align.CENTER
                }))
            }

            SectionLabel("Tamanho máximo")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PanelFont.entries.forEach { f ->
                    FilterChip(
                        selected = draft.maxFont == f.code,
                        onClick = { onChange(draft.copy(maxFont = f.code)) },
                        label = { Text(f.displayName, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
            Text(
                "O app usa este tamanho ou menor — o que couber na tela.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

        // Tamanho e liga/desliga vivem no SetupCard, no topo — aqui só o conteúdo.

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
        Card(colors = accentCardColors(Accent.Amber), border = accentBorder(Accent.Amber)) {
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
