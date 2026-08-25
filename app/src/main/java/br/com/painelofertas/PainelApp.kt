package br.com.painelofertas

import android.app.Application

/** Application: cria o [AppContainer] uma única vez para todo o app. */
class PainelApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
