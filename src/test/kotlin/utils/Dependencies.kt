package utils

import io.ktor.client.HttpClient
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.mockk.mockk
import core.Environment
import core.RuntimeEnvironments

fun Application.configureTestEnvironment() {
    dependencies {
        provide<HttpClient> { HttpClient() }
        provide<Environment>() {
            Environment(
                runtimeEnv = RuntimeEnvironments.LOCAL,
                kafka = mockk(),
                postgres = mockk(),
                texas = { mockk() },
                external = { mockk() },
            )
        }
    }
}
