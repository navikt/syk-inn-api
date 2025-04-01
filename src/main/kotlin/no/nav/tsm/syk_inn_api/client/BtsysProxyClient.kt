package no.nav.tsm.syk_inn_api.client

import no.nav.tsm.syk_inn_api.exception.BtsysException
import no.nav.tsm.syk_inn_api.service.TokenService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class BtsysProxyClient(
    webClientBuilder: WebClient.Builder,
    @Value("\${btsys.endpoint-url}") private val btsysEndpointUrl: String,
    private val tokenService: TokenService,
) {
    private val webClient: WebClient = webClientBuilder.baseUrl(btsysEndpointUrl).build()

    fun checkSuspensionStatus(sykmelderFnr: String, oppslagsdato: String): Boolean {
        val accessToken = tokenService.getTokenForBtsys().accessToken

        return try {
            val response = webClient.get()
                .uri { uriBuilder ->
                    uriBuilder.path("/api/v1/suspensjon/status")
                        .queryParam("oppslagsdato", oppslagsdato)
                        .build()
                }
                .headers {
                    it.set("Nav-Consumer-Id", "syk-inn-api")
                    it.set("Nav-Personident", sykmelderFnr)
                    it.set("Authorization", "Bearer $accessToken")
                }
                .retrieve()
                .onStatus({ status -> status.isError }, { response ->
                    throw BtsysException("Btsys responded with status: ${response.statusCode()}")
                })
                .bodyToMono(Boolean::class.java)
                .block()

            response ?: false
        } catch (e: Exception) {
            throw RuntimeException("Error while calling Btsys API", e)
        }
    }
}
