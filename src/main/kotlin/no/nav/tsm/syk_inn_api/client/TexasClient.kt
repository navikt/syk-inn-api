package no.nav.tsm.syk_inn_api.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.nio.file.AccessDeniedException
import javax.naming.AuthenticationException

/** Texas = Token Exchange as a Service */
@Component
class TexasClient(
    webClientBuilder: WebClient.Builder,
    @Value("\${nais.token.endpoint}") private val naisTokenEndpoint: String
) {
    private val webClient: WebClient = webClientBuilder.baseUrl(naisTokenEndpoint).build()

    fun requestToken(cluster: String, namespace: String, otherApiAppName: String): TokenResponse {
        val requestBody = TokenRequest(
            identityProvider = "azuread",
            target = "api://$cluster.$namespace.$otherApiAppName/.default"
        )

        return try {
            webClient.post()
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus({status -> status.is4xxClientError}) { response ->
                    response.bodyToMono(String::class.java).flatMap { errorBody ->
                        Mono.error(handleClientError(response.statusCode().value(), errorBody))
                    }
                }
                .onStatus({ status -> status.is5xxServerError }) { response ->
                    response.bodyToMono(String::class.java).flatMap { errorBody ->
                        Mono.error(RuntimeException("Server error (${response.statusCode()}): $errorBody"))
                    }
                }
                .bodyToMono(TokenResponse::class.java)
                .block() ?: throw RuntimeException("Failed to retrieve token: empty response")
        } catch (ex: WebClientResponseException) {
            throw RuntimeException("HTTP error: ${ex.statusCode} - ${ex.responseBodyAsString}", ex)
        } catch (ex: Exception) {
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

    data class TokenRequest(
        val identityProvider: String,
        val target: String
    )

    data class TokenResponse(
        val accessToken: String,
        val expiresIn: Int,
        val tokenType: String
    )
}
