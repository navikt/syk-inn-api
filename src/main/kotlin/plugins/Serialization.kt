package plugins

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.routing

fun Application.configureSerialization() {
    routing {
        install(ContentNegotiation) {
            jackson {
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                enable(SerializationFeature.INDENT_OUTPUT)
                registerModule(JavaTimeModule())
            }
        }
    }
}
