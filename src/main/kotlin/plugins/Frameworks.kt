package no.nav.tsm.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.CoroutineScope

fun Application.configureDependencies() {
    val config = environment.config

    dependencies { provide<CoroutineScope> { this } }
}
