package no.nav.tsm.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.CoroutineScope
import no.nav.tsm.core.Environment
import no.nav.tsm.core.initializeEnvironment

fun Application.configureDependencies() {
    val config = environment.config

    dependencies {
        provide<Environment>() { initializeEnvironment(config) }
        provide<CoroutineScope> { this }
    }
}
