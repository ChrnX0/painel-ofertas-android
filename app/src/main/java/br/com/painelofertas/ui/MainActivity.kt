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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.painelofertas.PainelApp
import br.com.painelofertas.R
import br.com.painelofertas.nfc.NfcReader
import br.com.painelofertas.ui.components.StatusPill
import br.com.painelofertas.ui.screens.AgendaScreen
import br.com.painelofertas.ui.screens.ConfigScreen
import br.com.painelofertas.ui.screens.EditarScreen
import br.com.painelofertas.ui.screens.EnviarScreen
import br.com.painelofertas.ui.screens.PaineisScreen
import br.com.painelofertas.ui.theme.PainelOfertasTheme
import br.com.painelofertas.ui.vm.AppViewModel
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
                PainelOfertasApp()
            }
        }
    }
}

private enum class Destino(val label: String, val icon: ImageVector) {
    EDITAR("Editar", Icons.Filled.Edit),
    ENVIAR("Enviar", Icons.AutoMirrored.Filled.Send),
    PAINEIS("Painéis", Icons.Filled.Tv),
    AGENDA("Agenda", Icons.Filled.CalendarMonth),
    CONFIG("Config", Icons.Filled.Settings),
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

    // Voltar: fecha a gaveta se aberta; senão volta pra 1ª tela.
    BackHandler(enabled = drawerState.isOpen || atual != 0) {
        if (drawerState.isOpen) drawerScope.launch { drawerState.close() } else nav.selectedTab = 0
    }

    val snackbar = remember { SnackbarHostState() }

    // Painel identificado por NFC: avisa e leva direto para a lista de painéis.
    LaunchedEffect(nfcAviso) {
        if (nfcAviso.isNotBlank()) {
            nav.selectedTab = 2
            snackbar.showSnackbar(nfcAviso)
            nfcAviso = ""
        }
    }

    CompositionLocalProvider(LocalSnackbar provides snackbar) {
        ModalNavigationDrawer(
            drawerState = drawerState,
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(8.dp))
                    destinos.forEachIndexed { index, d ->
                        NavigationDrawerItem(
                            icon = { Icon(d.icon, null) },
                            label = { Text(d.label) },
                            selected = atual == index,
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
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.055f),
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.background,
                                ),
                            ),
                        ),
                ) {
                    AnimatedContent(
                        targetState = atual,
                        transitionSpec = {
                            val dir = if (targetState > initialState) 1 else -1
                            (fadeIn(tween(280)) + slideInHorizontally(tween(340)) { w -> dir * w / 10 }) togetherWith
                                (fadeOut(tween(180)) + slideOutHorizontally(tween(340)) { w -> -dir * w / 10 })
                        },
                        modifier = Modifier.align(Alignment.TopCenter).widthIn(max = 700.dp).fillMaxSize(),
                        label = "screen",
                    ) { idx ->
                        when (destinos[idx]) {
                            Destino.EDITAR -> EditarScreen()
                            Destino.ENVIAR -> EnviarScreen()
                            Destino.PAINEIS -> PaineisScreen()
                            Destino.AGENDA -> AgendaScreen()
                            Destino.CONFIG -> ConfigScreen()
                        }
                    }
                }
            }
        }
    }
}
