package no.nav.tsm.syk_inn_api.security

import java.nio.file.AccessDeniedException
import javax.naming.AuthenticationException
import no.nav.tsm.syk_inn_api.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException

/** Texas = Token Exchange as a Service */
@Profile("!local")
@Component
class TexasClient(
    restClientBuilder: RestClient.Builder,
    @param:Value($$"${nais.token_endpoint}") private val naisTokenEndpoint: String,
    @param:Value($$"${nais.cluster}") private val cluster: String,
) {
    private val restClient: RestClient = restClientBuilder.baseUrl(naisTokenEndpoint).build()
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
            val response =
                restClient
                    .post()
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(TokenResponse::class.java)

            response ?: throw IllegalStateException("Failed to retrieve token: empty response")
        } catch (e: RestClientResponseException) {
            val status = e.statusCode
            val body = e.responseBodyAsString
            logger.error(
                "TexasClient token request failed with HTTP ${status.value()}: $body (ns=$namespace app=$otherApiAppName)",
                e,
            )
            throw mapTexasError(status, body, e)
        } catch (e: RestClientException) {
            logger.error(
                "Unexpected RestClientException while requesting token for ${namespace}:${otherApiAppName}",
                e,
            )
            throw RuntimeException("Unexpected error: ${e.message}", e)
        } catch (e: Exception) {
            logger.error(
                "Unexpected exception while requesting token for ${namespace}:${otherApiAppName}",
                e,
            )
            throw e
        }
    }

    private fun mapTexasError(
        status: HttpStatusCode,
        errorBody: String,
        cause: Throwable?
    ): Exception =
        when (status.value()) {
            400 -> IllegalArgumentException("Bad Request: $errorBody", cause)
            401 -> AuthenticationException("Unauthorized: $errorBody").apply { initCause(cause) }
            403 -> AccessDeniedException("Forbidden: $errorBody").apply { initCause(cause) }
            in 500..599 -> RuntimeException("Server error (${status.value()}): $errorBody", cause)
            else -> RuntimeException("HTTP ${status.value()}: $errorBody", cause)
        }

    data class TokenRequest(val identity_provider: String, val target: String)

    data class TokenResponse(val access_token: String, val expires_in: Int, val token_type: String)
}
