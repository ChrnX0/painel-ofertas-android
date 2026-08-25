package br.com.painelofertas.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import br.com.painelofertas.AppContainer
import br.com.painelofertas.PainelApp

/** Acesso ao [AppContainer] a partir de qualquer Composable. */
@Composable
fun rememberContainer(): AppContainer =
    (LocalContext.current.applicationContext as PainelApp).container

/** Snackbar compartilhado do app (avisos + "Desfazer"). */
val LocalSnackbar = staticCompositionLocalOf { SnackbarHostState() }
