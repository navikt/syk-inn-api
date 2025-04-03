package no.nav.tsm.syk_inn_api.service

import no.nav.tsm.syk_inn_api.client.TexasClient
import org.springframework.stereotype.Service

@Service
class TokenService(
    private val texasClient: TexasClient,
) {
    fun getTokenForBtsys(): TexasClient.TokenResponse {
        val cluster = "dev-gcp" // TODO make properties for these
        val namespace = "team-rocket"
        val appName = "btsys-api"
        return texasClient.requestToken(cluster, namespace, appName)
    }

    fun getTokenForPdl(): TexasClient.TokenResponse {
        val cluster = "dev-gcp" // TODO make properties for these
        val namespace = "tsm"
        val appName = "tsm-pdl-cache"
        return texasClient.requestToken(cluster, namespace, appName)
    }
}
