package no.nav.tsm.syk_inn_api.client

import no.nav.tsm.syk_inn_api.exception.PdlException
import no.nav.tsm.syk_inn_api.service.TokenService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate

@Component
class PdlClient(
    webClientBuilder: WebClient.Builder,
    @Value("\${pdlcache.endpoint-url}") private val pdlEndpointUrl: String,
    private val tokenService: TokenService,
) {
    private val webClient = webClientBuilder.baseUrl(pdlEndpointUrl).build()

    fun getFodselsdato(fnr: String): LocalDate {
        val accessToken = tokenService.getTokenForPdl().accessToken

        return try {
            webClient.get().uri{
                it.path("/pdl/v1/fodselsdato")
                    .queryParam("fnr", fnr)
                    .build()
            }
            val response = webClient.get()
                .uri { uriBuilder ->
                    uriBuilder.path("/pdl/v1/fodselsdato")
                        .queryParam("fnr", fnr)
                        .build()
                }
                .headers {
//                    it.set("Nav-Call-Id", "syk-inn-api")
                    it.set("Nav-Consumer-Id", "syk-inn-api")
                    it.set("Authorization", "Bearer $accessToken")
                }
                .retrieve()
                .onStatus({ status -> status.isError }, { response ->
                    throw PdlException("Pdl responded with status: ${response.statusCode()}")
                })
                .bodyToMono(LocalDate::class.java)
                .block()

            response ?: throw RuntimeException("Pdl returned no birth date for fnr=$fnr")
        } catch (e: Exception) {
            throw RuntimeException("Error while calling Pdl API", e) //TODO handle error differently, maybe return null instead of throwing. we dont want to control flow with throws. fix this.
        }
    }
}

