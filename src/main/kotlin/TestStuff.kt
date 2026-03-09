package no.nav.tsm

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import modules.sykmeldinger.SykmeldingerService
import modules.sykmeldinger.pdl.PdlClient

fun Application.configureTestStuff() {
    val sir: SykmeldingerService by dependencies
    val pdl: PdlClient by dependencies

    routing {
        route("/test") {
            configureRouteSerialization()

            /** TODO Only test endpoints */
            get {
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
}

fun Route.configureRouteSerialization() {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            registerModule(JavaTimeModule())
        }
    }
}
