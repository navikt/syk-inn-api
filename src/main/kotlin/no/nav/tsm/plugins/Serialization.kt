package no.nav.tsm.plugins

import io.ktor.serialization.jackson3.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import tools.jackson.databind.SerializationFeature

fun Application.configureSerialization() {
    install(ContentNegotiation) { jackson { enable(SerializationFeature.INDENT_OUTPUT) } }
}
