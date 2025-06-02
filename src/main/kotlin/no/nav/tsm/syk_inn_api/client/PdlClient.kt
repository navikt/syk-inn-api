package no.nav.tsm.syk_inn_api.client

import no.nav.tsm.syk_inn_api.exception.PdlException
import no.nav.tsm.syk_inn_api.exception.PersonNotFoundException
import no.nav.tsm.syk_inn_api.model.PdlPerson
import no.nav.tsm.syk_inn_api.service.TokenService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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

@Profile("!local")
@Component
class PdlClient(
    webClientBuilder: WebClient.Builder,
    @Value("\${pdlcache.endpoint-url}") private val pdlEndpointUrl: String,
    private val tokenService: TokenService,
) : IPdlClient {
    private val webClient = webClientBuilder.baseUrl(pdlEndpointUrl).build()
    private val logger = LoggerFactory.getLogger(IPdlClient::class.java)
    private val secureLog: Logger = LoggerFactory.getLogger("securelog")

    override fun getPerson(fnr: String): Result<PdlPerson> {
        val accessToken = tokenService.getTokenForPdl().access_token
        return try {
            val response =
                webClient
                    .get()
                    .uri { uriBuilder ->
                        uriBuilder.path("/api/person").build()
                    }
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
                                    PdlException(
                                        "PDL responded with status: ${response.statusCode()} and body: $body"
                                    )
                                secureLog.error(
                                    "Error while fetching person with fnr $fnr from PDL cache",
                                    ex
                                )
                                Mono.error(ex)
                            }
                        },
                    )
                    .bodyToMono(PdlPerson::class.java)
                    .block()
            if (response != null) {
                Result.Success(response)
            } else {
                Result.Failure(PdlException("Pdl cache did not return a person"))
            }
        } catch (e: HttpClientErrorException.NotFound) {
            secureLog.warn("Person with fnr $fnr not found in PDL cache", e)
            logger.warn("PDL person not found in PDL cache", e)
            throw PersonNotFoundException("Could not find person in pdl cache")
        } catch (e: Exception) {
            logger.error("Error while calling Pdl API", e)
            Result.Failure(e)
        }
    }
}
