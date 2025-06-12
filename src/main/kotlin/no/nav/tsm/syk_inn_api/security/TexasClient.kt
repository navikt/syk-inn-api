package no.nav.tsm.syk_inn_api.security

import java.nio.file.AccessDeniedException
import javax.naming.AuthenticationException
import no.nav.tsm.syk_inn_api.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

/** Texas = Token Exchange as a Service */
@Profile("!local")
@Component
class TexasClient(
    webClientBuilder: WebClient.Builder,
    @Value("\${nais.token_endpoint}") private val naisTokenEndpoint: String,
    @Value("\${nais.cluster}") private val cluster: String,
) {
    private val webClient: WebClient = webClientBuilder.baseUrl(naisTokenEndpoint).build()
    private val logger = logger()

    fun requestToken(namespace: String, otherApiAppName: String): TokenResponse {
        logger.info(
            "Requesting token for $otherApiAppName in namespace $namespace on cluster $cluster and endpoint $naisTokenEndpoint",
        )
        val requestBody =
            TokenRequest(
                identity_provider = "azuread",
                target = "api://$cluster.$namespace.$otherApiAppName/.default",
            )

        return try {
            logger.info("Trying to request token with body: $requestBody")
            webClient
                .post()
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus({ status -> status.is4xxClientError }) { response ->
                    response.bodyToMono(String::class.java).flatMap { errorBody ->
                        logger.error(
                            "TexasClient got a Client error: ${response.statusCode()} - $errorBody",
                        )
                        Mono.error(handleClientError(response.statusCode().value(), errorBody))
                    }
                }
                .onStatus({ status -> status.is5xxServerError }) { response ->
                    response.bodyToMono(String::class.java).flatMap { errorBody ->
                        logger.error(
                            "TexasClient got a Server error: ${response.statusCode()} - $errorBody",
                        )
                        Mono.error(
                            RuntimeException("Server error (${response.statusCode()}): $errorBody"),
                        )
                    }
                }
                .bodyToMono(TokenResponse::class.java)
                .block()
                ?: throw RuntimeException("Failed to retrieve token: empty response")
        } catch (ex: WebClientResponseException) {
            logger.error(
                "WebClientResponseException: ${ex.statusCode} - ${ex.responseBodyAsString}",
                ex,
            )
            throw RuntimeException("HTTP error: ${ex.statusCode} - ${ex.responseBodyAsString}", ex)
        } catch (ex: Exception) {
            logger.error("Unexpected error while requesting token: ${ex.message}", ex)
            throw RuntimeException("Unexpected error: ${ex.message}", ex)
        }
    }

    private fun handleClientError(status: Int, errorBody: String): Exception {
        return when (status) {
            400 -> IllegalArgumentException("Bad Request: $errorBody")
            401 -> AuthenticationException("Unauthorized: $errorBody")
            403 -> AccessDeniedException("Forbidden: $errorBody")
            else -> RuntimeException("Client error ($status): $errorBody")
        }
    }

    data class TokenRequest(val identity_provider: String, val target: String)

    data class TokenResponse(val access_token: String, val expires_in: Int, val token_type: String)
}
