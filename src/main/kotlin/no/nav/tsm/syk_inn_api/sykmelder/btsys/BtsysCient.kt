package no.nav.tsm.syk_inn_api.sykmelder.btsys

import java.util.*
import no.nav.tsm.syk_inn_api.exception.BtsysException
import no.nav.tsm.syk_inn_api.security.TexasClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

interface IBtsysClient {
    fun checkSuspensionStatus(sykmelderFnr: String, oppslagsdato: String): Result<Suspendert>
}

@Profile("!local")
@Component
class BtsysCient(
    webClientBuilder: WebClient.Builder,
    private val texasClient: TexasClient,
    @Value("\${btsys.endpoint-url}") private val btsysEndpointUrl: String,
) : IBtsysClient {
    private val webClient: WebClient = webClientBuilder.baseUrl(btsysEndpointUrl).build()
    private val logger = LoggerFactory.getLogger(BtsysCient::class.java)

    override fun checkSuspensionStatus(
        sykmelderFnr: String,
        oppslagsdato: String
    ): Result<Suspendert> {
        val (accessToken) = this.getToken()

        val loggId = UUID.randomUUID().toString()
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
                        it.set("Nav-Call-Id", loggId)
                        it.set("Nav-Consumer-Id", "syk-inn-api")
                        it.set("Nav-Personident", sykmelderFnr)
                        it.set("Authorization", "Bearer $accessToken")
                        it.set("Accept", "application/json")
                    }
                    .retrieve()
                    .onStatus(
                        { status -> status.isError },
                        { response ->
                            response.bodyToMono(String::class.java).flatMap { body ->
                                logger.error(
                                    "Btsys responded with status: ${response.statusCode()}, body: $body",
                                )
                                Mono.error(
                                    BtsysException(
                                        "Btsys responded with status: ${response.statusCode()}, body: $body",
                                    ),
                                )
                            }
                        },
                    )
                    .bodyToMono(Suspendert::class.java)
                    .block()

            if (response != null) {
                Result.success(response)
            } else {
                Result.failure(BtsysException("Btsys returned no suspension status"))
            }
        } catch (e: Exception) {
            logger.error("Error while calling Btsys API", e)
            Result.failure(e)
        }
    }

    private fun getToken(): TexasClient.TokenResponse =
        texasClient.requestToken("team-rocket", "btsys-api")
}

data class Suspendert(val suspendert: Boolean)
