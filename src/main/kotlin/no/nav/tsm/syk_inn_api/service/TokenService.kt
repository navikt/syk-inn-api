package no.nav.tsm.syk_inn_api.service

import no.nav.tsm.syk_inn_api.client.TexasClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TokenService(
    private val texasClient: TexasClient,
    @Value("\${nais.cluster}") private val clusterName: String,
) {
    fun getTokenForBtsys(): TexasClient.TokenResponse {
        val cluster = clusterName
        val namespace = "team-rocket"
        val appName = "btsys-api"
        return texasClient.requestToken(cluster, namespace, appName)
    }

    fun getTokenForPdl(): TexasClient.TokenResponse {
        val cluster = clusterName
        val namespace = "tsm"
        val appName = "tsm-pdl-cache"
        return texasClient.requestToken(cluster, namespace, appName)
    }

    fun getTokenForHelsenettProxy(): TexasClient.TokenResponse {
        val cluster = clusterName
        val namespace = "teamsykmelding"
        val appName = "syfohelsenettproxy"
        return texasClient.requestToken(cluster, namespace, appName)
    }
}
