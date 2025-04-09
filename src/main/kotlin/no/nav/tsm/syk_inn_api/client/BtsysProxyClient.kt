package no.nav.tsm.syk_inn_api.client

import no.nav.tsm.syk_inn_api.exception.BtsysException
import no.nav.tsm.syk_inn_api.service.TokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

interface IBtsysClient {
    fun checkSuspensionStatus(sykmelderFnr: String, oppslagsdato: String): Result<Boolean>
}

@Profile("!local")
@Component
class BtsysProxyClient(
    webClientBuilder: WebClient.Builder,
    @Value("\${btsys.endpoint-url}") private val btsysEndpointUrl: String,
    private val tokenService: TokenService,
) : IBtsysClient {
    private val webClient: WebClient = webClientBuilder.baseUrl(btsysEndpointUrl).build()
    private val logger = LoggerFactory.getLogger(BtsysProxyClient::class.java)

    override fun checkSuspensionStatus(
        sykmelderFnr: String,
        oppslagsdato: String
    ): Result<Boolean> {
        val accessToken = tokenService.getTokenForBtsys().accessToken

        return try {
            val response =
                webClient
                    .get()
                    .uri { uriBuilder ->
                        uriBuilder
                            .path("/api/v1/suspensjon/status")
                            .queryParam("oppslagsdato", oppslagsdato)
                            .build()
                    }
                    .headers {
                        it.set("Nav-Consumer-Id", "syk-inn-api")
                        it.set("Nav-Personident", sykmelderFnr)
                        it.set("Authorization", "Bearer $accessToken")
                    }
                    .retrieve()
                    .onStatus(
                        { status -> status.isError },
                        { response ->
                            throw BtsysException(
                                "Btsys responded with status: ${response.statusCode()}"
                            )
                        }
                    )
                    .bodyToMono(Boolean::class.java)
                    .block()

            if (response != null) {
                Result.Success(response)
            } else {
                Result.Failure(BtsysException("Btsys returned no suspension status"))
            }
        } catch (e: Exception) {
            logger.error("Error while calling Btsys API", e)
            Result.Failure(e)
        }
    }
}
