package plugins

import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.routing.*

fun Application.configureOpenAPI() {
    routing {
        openAPI(path = "openapi") {
            info = OpenApiInfo(
                title = "Syk Inn API",
                version = "1.0.0",
                description = "Syk inn API is a direct API for syk-inn only accessible through service discovery in the nais k8s cluster."
            )
        }
    }
}
