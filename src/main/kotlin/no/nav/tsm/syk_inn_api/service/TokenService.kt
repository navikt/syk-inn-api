package no.nav.tsm.syk_inn_api.service

import no.nav.tsm.syk_inn_api.client.TexasClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TokenService(
    private val texasClient: TexasClient,
    @Value("\${nais.cluster}") private val clusterName: String,
) {
    private val logger = LoggerFactory.getLogger(TokenService::class.java)

    fun getTokenForBtsys(): TexasClient.TokenResponse {
        logger.info("Requesting token for btsys")
        val cluster = clusterName
        val namespace = "team-rocket"
        val appName = "btsys-api"
        return texasClient.requestToken(cluster, namespace, appName)
    }

    fun getTokenForPdl(): TexasClient.TokenResponse {
        logger.info("Requesting token for PDL")
        val cluster = clusterName
        val namespace = "tsm"
        val appName = "tsm-pdl-cache"
        return texasClient.requestToken(cluster, namespace, appName)
    }

    fun getTokenForHelsenettProxy(): TexasClient.TokenResponse {
        logger.info("Requesting token for helsenett proxy")
        val cluster = clusterName
        val namespace = "teamsykmelding"
        val appName = "syfohelsenettproxy"
        val res = texasClient.requestToken(cluster, namespace, appName)
        logger.info("Received token for helsenett proxy: ${res.access_token.take(10)}...")
        return res
    }
}
