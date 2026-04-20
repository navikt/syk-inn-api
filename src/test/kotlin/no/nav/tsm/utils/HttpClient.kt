package no.nav.tsm.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine

fun MockEngine.client(): HttpClient = HttpClient(this) {}
