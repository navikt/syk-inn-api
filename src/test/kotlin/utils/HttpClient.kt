package utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson

fun MockEngine.client(): HttpClient = HttpClient(this) { install(ContentNegotiation) { jackson() } }
