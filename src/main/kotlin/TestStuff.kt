package no.nav.tsm

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import modules.external.clients.pdl.PdlClient
import no.nav.tsm.modules.sykmeldinger.SykmeldingService
import plugins.configureSerialization

fun Application.configureTestStuff() {
    val sir: SykmeldingService by dependencies
    val pdl: PdlClient by dependencies

    configureSerialization()

    routing {
        /** TODO Only test endpoints */
        get("/test") {
            println(pdl.getPerson("123213123"))

            val sykmeldinger = sir.test()

            call.respond(HttpStatusCode.Created, sykmeldinger)
        }
        post("/create-boio") {
            val newSykmelding = sir.createBoio()

            call.respond(HttpStatusCode.Created, newSykmelding)
        }
    }
}
