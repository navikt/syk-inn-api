package no.nav.tsm.syk_inn_api.person.pdl

import no.nav.tsm.syk_inn_api.security.TexasClient
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.syk_inn_api.utils.secureLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

interface IPdlClient {
    fun getPerson(fnr: String): Result<PdlPerson>
}

@Profile("!local & !test")
@Component
class PdlClient(
    webClientBuilder: WebClient.Builder,
    private val texasClient: TexasClient,
    @Value("\${services.teamsykmelding.pdlcache.url}") private val pdlEndpointUrl: String,
) : IPdlClient {
    private val webClient = webClientBuilder.baseUrl(pdlEndpointUrl).build()
    private val logger = logger()
    private val secureLog = secureLogger()

    override fun getPerson(fnr: String): Result<PdlPerson> {
        val (accessToken) = getToken()

        return try {
            val response =
                webClient
                    .get()
                    .uri { uriBuilder -> uriBuilder.path("/api/person").build() }
                    .headers {
                        it.set("Nav-Consumer-Id", "syk-inn-api")
                        it.set("Authorization", "Bearer $accessToken")
                        it.set("Ident", fnr)
                    }
                    .retrieve()
                    .onStatus(
                        { status -> status.isError },
                        { response ->
                            response.bodyToMono<String>().defaultIfEmpty("").flatMap { body ->
                                val ex =
                                    IllegalStateException(
                                        "PDL responded with status: ${response.statusCode()} and body: $body",
                                    )

                                secureLog.error(
                                    "Error while fetching person with fnr $fnr from PDL cache",
                                    ex,
                                )
                                Mono.error(ex)
                            }
                        },
                    )
                    .bodyToMono(PdlPerson::class.java)
                    .block()
            if (response != null) {
                Result.success(response)
            } else {
                Result.failure(IllegalStateException("Pdl cache did not return a person"))
            }
        } catch (e: HttpClientErrorException.NotFound) {
            secureLog.warn("Person with fnr $fnr not found in PDL cache", e)
            logger.warn("PDL person not found in PDL cache", e)
            throw IllegalStateException("Could not find person in pdl cache")
        } catch (e: Exception) {
            logger.error("Error while calling Pdl API", e)
            Result.failure(e)
        }
    }

    private fun getToken(): TexasClient.TokenResponse =
        texasClient.requestToken("tsm", "tsm-pdl-cache")
}
