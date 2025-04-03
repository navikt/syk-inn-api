package no.nav.tsm.syk_inn_api.client

import java.time.LocalDate
import no.nav.tsm.syk_inn_api.exception.PdlException
import no.nav.tsm.syk_inn_api.service.TokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class PdlClient(
    webClientBuilder: WebClient.Builder,
    @Value("\${pdlcache.endpoint-url}") private val pdlEndpointUrl: String,
    private val tokenService: TokenService,
) {
    private val webClient = webClientBuilder.baseUrl(pdlEndpointUrl).build()
    private val logger = LoggerFactory.getLogger(PdlClient::class.java)

    sealed class Result<out T> {
        data class Success<out T>(val data: T) : Result<T>()

        data class Failure(val error: Throwable) : Result<Nothing>()
    }

    fun getFodselsdato(fnr: String): Result<LocalDate> {
        val accessToken = tokenService.getTokenForPdl().accessToken
        return try {
            val response =
                webClient
                    .get()
                    .uri { uriBuilder ->
                        uriBuilder.path("/pdl/v1/fodselsdato").queryParam("fnr", fnr).build()
                    }
                    .headers {
                        it.set("Nav-Consumer-Id", "syk-inn-api")
                        it.set("Authorization", "Bearer $accessToken")
                    }
                    .retrieve()
                    .onStatus(
                        { status -> status.isError },
                        { response ->
                            throw PdlException(
                                "Pdl responded with status: ${response.statusCode()}",
                            )
                        },
                    )
                    .bodyToMono(LocalDate::class.java)
                    .block()

            if (response != null) {
                Result.Success(response)
            } else {
                Result.Failure(PdlException("Pdl returned no birth date"))
            }
        } catch (e: Exception) {
            logger.error("Error while calling Pdl API", e)
            Result.Failure(e)
        }
    }
}
