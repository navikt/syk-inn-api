package utils

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder

fun ApplicationTestBuilder.testClient(): HttpClient {
    return createClient { install(ContentNegotiation) { jackson() } }
}
