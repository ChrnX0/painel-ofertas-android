package br.com.painelofertas.ui

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.painelofertas.PainelApp
import br.com.painelofertas.R
import br.com.painelofertas.ui.components.StatusPill
import br.com.painelofertas.ui.screens.AgendaScreen
import br.com.painelofertas.ui.screens.ConfigScreen
import br.com.painelofertas.ui.screens.EditarScreen
import br.com.painelofertas.ui.screens.EnviarScreen
import br.com.painelofertas.ui.screens.PaineisScreen
import br.com.painelofertas.ui.theme.PainelOfertasTheme
import br.com.painelofertas.ui.vm.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
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

    // Botão voltar: se não estiver na 1ª aba, volta pra ela em vez de fechar o app.
    BackHandler(enabled = atual != 0) { nav.selectedTab = 0 }

    val snackbar = remember { SnackbarHostState() }
    CompositionLocalProvider(LocalSnackbar provides snackbar) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        Modifier
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.logo_ledblock_trim),
                            contentDescription = "LedBlock",
                            modifier = Modifier.height(22.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                },
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp),
                    ) {
                        StatusPill(phase = wifiPhase, label = "Wi-Fi")
                        StatusPill(phase = usbPhase, label = "USB")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                destinos.forEachIndexed { index, d ->
                    NavigationBarItem(
                        selected = atual == index,
                        onClick = { nav.selectedTab = index },
                        icon = { Icon(d.icon, contentDescription = d.label) },
                        label = { Text(d.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        // Coluna com largura de leitura: no celular ocupa tudo; no tablet centraliza
        // (mata o "oceano de preto" das laterais).
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
