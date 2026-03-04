package no.nav.tsm.modules.external.clients.texas

import io.ktor.client.HttpClient
import io.ktor.server.plugins.di.annotations.Named
import modules.external.clients.texas.TexasClient
import modules.external.clients.texas.TexasToken
import no.nav.tsm.core.Environment

class TexasLocalClient(
    @Named("RetryHttpClient") private val httpClient: HttpClient,
    private val env: Environment,
) : TexasClient(httpClient = httpClient, env = env) {
    override suspend fun requestToken(namespace: String, otherApiAppName: String): TexasToken {
        return TexasToken("fake-token-for-${namespace}-${otherApiAppName}")
    }
}
