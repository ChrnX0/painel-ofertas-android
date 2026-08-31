package br.com.painelofertas.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.painelofertas.PainelApp
import br.com.painelofertas.R
import br.com.painelofertas.nfc.NfcReader
import br.com.painelofertas.ui.components.Accent
import br.com.painelofertas.ui.components.AuroraBackground
import br.com.painelofertas.ui.components.Motion
import br.com.painelofertas.ui.components.Appear
import br.com.painelofertas.ui.components.BreathProvider
import br.com.painelofertas.ui.components.ButtonShape
import br.com.painelofertas.ui.components.SoftDivider
import br.com.painelofertas.ui.components.StatusPill
import br.com.painelofertas.ui.screens.AgendaScreen
import br.com.painelofertas.ui.screens.ConfigScreen
import br.com.painelofertas.ui.screens.EditarScreen
import br.com.painelofertas.ui.screens.PaineisScreen
import br.com.painelofertas.ui.theme.PainelOfertasTheme
import br.com.painelofertas.ui.vm.AppViewModel
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

/** Aviso da última leitura NFC — a UI mostra e limpa. */
private var nfcAviso by mutableStateOf("")

class MainActivity : ComponentActivity() {

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        tratarNfc(intent) // celular encostou num painel com etiqueta
    }

    /** Identifica o painel pela etiqueta NFC e o registra/atualiza na lista. */
    private fun tratarNfc(intent: Intent?) {
        val tag = NfcReader.fromIntent(intent) ?: return
        val container = (applicationContext as PainelApp).container
        container.panels.upsertFromTag(tag.id, tag.nome, tag.ip, tag.grupo)
        nfcAviso = "Painel identificado por NFC: ${tag.nome.ifBlank { tag.id }}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        tratarNfc(intent)
        setContent {
            val container = (applicationContext as PainelApp).container
            val themeMode by container.settings.themeMode.collectAsState()
            val dark = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            PainelOfertasTheme(darkTheme = dark) {
                // Um pulmão só para o app inteiro — ver BreathProvider.
                BreathProvider { PainelOfertasApp() }
            }
        }
    }
}

/**
 * A antiga aba "Enviar" foi absorvida pela barra Publicar do editor: o destino
 * (um painel, vários, um grupo ou o USB) se escolhe no momento de publicar, sem
 * o usuário ter que salvar um álbum, trocar de tela e reencontrá-lo lá.
 */
private enum class Destino(val label: String, val icon: ImageVector) {
    EDITAR("Editar", Icons.Filled.Edit),
    PAINEIS("Painéis", Icons.Filled.Tv),
    AGENDA("Agenda", Icons.Filled.CalendarMonth),
    CONFIG("Config", Icons.Filled.Settings),
}

/**
 * Trilho fino de posição sob a barra superior.
 *
 * Sem a barra de abas de antes, o gesto de arrastar seria invisível — este trilho
 * é a única pista de que existem outras telas ao lado. O cursor lê
 * `currentPage + currentPageOffsetFraction`, ou seja, a posição **contínua** do
 * pager: ele acompanha o dedo durante o arraste, em vez de pular quando a página
 * troca. É essa continuidade que faz o gesto parecer que move a interface, e não
 * que dispara uma animação.
 */
@Composable
private fun PageRail(pager: PagerState, total: Int) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).height(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(total) { i ->
            // Distância contínua até esta posição: 0 = bem em cima, 1 = longe.
            val d = ((pager.currentPage - i) + pager.currentPageOffsetFraction)
                .absoluteValue.coerceIn(0f, 1f)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .drawBehind {
                        drawRect(lerp(cs.outlineVariant, cs.primary, 1f - d))
                    },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PainelOfertasApp() {
    val container = rememberContainer()
    val nav: AppViewModel = viewModel()
    val destinos = Destino.entries
    val atual = nav.selectedTab.coerceIn(0, destinos.lastIndex)
    val wifiPhase by container.connection.wifi.collectAsState()
    val usbPhase by container.connection.usb.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()

    // Arrastar para o lado troca de tela. O pager e a gaveta são duas fontes da
    // mesma verdade, então sincronizamos os dois sentidos — sempre comparando
    // antes de agir, senão um dispara o outro em laço.
    val pager = rememberPagerState(initialPage = atual) { destinos.size }
    LaunchedEffect(nav.selectedTab) {
        if (pager.currentPage != nav.selectedTab) pager.animateScrollToPage(nav.selectedTab)
    }
    LaunchedEffect(pager.settledPage) {
        // settledPage (e não currentPage) para o menu só marcar a tela quando o
        // dedo solta e o pager assenta — no meio do arraste ainda dá para voltar.
        if (nav.selectedTab != pager.settledPage) nav.selectedTab = pager.settledPage
    }

    // Voltar: fecha a gaveta se aberta; senão volta pra 1ª tela.
    BackHandler(enabled = drawerState.isOpen || atual != 0) {
        if (drawerState.isOpen) drawerScope.launch { drawerState.close() } else nav.selectedTab = 0
    }

    val snackbar = remember { SnackbarHostState() }

    // Painel identificado por NFC: avisa e leva direto para a lista de painéis.
    LaunchedEffect(nfcAviso) {
        if (nfcAviso.isNotBlank()) {
            nav.selectedTab = AppViewModel.TAB_PAINEIS
            snackbar.showSnackbar(nfcAviso)
            nfcAviso = ""
        }
    }

    CompositionLocalProvider(LocalSnackbar provides snackbar) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            // Fechada, a gaveta abre mão do gesto: ela captura arrasto horizontal em
            // toda a área de conteúdo e engoliria o do pager. Aberta, o gesto volta,
            // para poder fechá-la arrastando. Abrir continua no botão sanduíche.
            gesturesEnabled = drawerState.isOpen,
            drawerContent = {
                ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Box(
                            Modifier.background(Color.White, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Image(
                                painterResource(R.drawable.logo_ledblock_trim), "LedBlock",
                                Modifier.height(26.dp), contentScale = ContentScale.Fit,
                            )
                        }
                        Text(
                            "Painel de Ofertas",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp, start = 2.dp),
                        )
                    }
                    SoftDivider()
                    Spacer(Modifier.height(8.dp))
                    // Cada item entra um pouco depois do anterior: a gaveta se
                    // desdobra em vez de aparecer inteira.
                    destinos.forEachIndexed { index, d ->
                        Appear(delayMillis = 40 + index * 45) {
                            NavigationDrawerItem(
                                icon = { Icon(d.icon, null) },
                                label = { Text(d.label) },
                                selected = atual == index,
                                shape = ButtonShape,
                                onClick = { nav.selectedTab = index; drawerScope.launch { drawerState.close() } },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                            )
                        }
                    }
                }
            },
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbar) },
                topBar = {
                    CenterAlignedTopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { drawerScope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, "Abrir menu")
                            }
                        },
                        title = {
                            Box(
                                Modifier.background(Color.White, RoundedCornerShape(9.dp)).padding(horizontal = 12.dp, vertical = 7.dp),
                            ) {
                                Image(
                                    painterResource(R.drawable.logo_ledblock_trim), "LedBlock",
                                    Modifier.height(28.dp), contentScale = ContentScale.Fit,
                                )
                            }
                        },
                        actions = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 10.dp),
                            ) {
                                StatusPill(wifiPhase, Icons.Filled.Wifi, "Wi-Fi")
                                StatusPill(usbPhase, Icons.Filled.Usb, "USB")
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                },
            ) { innerPadding ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    // Fundo vivo: manchas pastéis derivando devagar. Fica atrás de
                    // tudo e é sentido mais do que visto.
                    AuroraBackground(
                        Modifier.matchParentSize(),
                        colors = listOf(Accent.Sky, Accent.Lilac, Accent.Teal),
                    )

                    Column(Modifier.align(Alignment.TopCenter).widthIn(max = 700.dp).fillMaxSize()) {
                        // Trilho de posição: mostra onde você está e, principalmente,
                        // **revela que dá para arrastar**. Sem a barra de abas embaixo,
                        // o gesto seria invisível.
                        PageRail(pager, destinos.size)

                        HorizontalPager(
                            state = pager,
                            modifier = Modifier.fillMaxSize(),
                            // Só a página atual fica composta: as telas são pesadas
                            // (o editor desenha milhares de LEDs) e manter vizinhas
                            // vivas custaria caro por nada.
                            beyondViewportPageCount = 0,
                            pageContent = { idx ->
                                // Profundidade no arraste: a página que sai encolhe e
                                // esmaece, a que entra cresce até o lugar. O movimento
                                // acompanha o dedo em vez de tocar depois dele.
                                val dist = ((pager.currentPage - idx) + pager.currentPageOffsetFraction)
                                    .absoluteValue.coerceIn(0f, 1f)
                                Box(
                                    Modifier.fillMaxSize().graphicsLayer {
                                        val e = 1f - dist
                                        scaleX = 0.88f + 0.12f * e
                                        scaleY = 0.88f + 0.12f * e
                                        alpha = 0.35f + 0.65f * e
                                    },
                                ) {
                                    when (destinos[idx]) {
                                        Destino.EDITAR -> EditarScreen()
                                        Destino.PAINEIS -> PaineisScreen()
                                        Destino.AGENDA -> AgendaScreen()
                                        Destino.CONFIG -> ConfigScreen()
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
