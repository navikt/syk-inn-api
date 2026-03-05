package modules.sykmelder.clients.texas

import core.Environment
import core.logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.isTextType
import io.ktor.server.plugins.di.annotations.Named
import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan

sealed interface TexasClient {
    suspend fun requestToken(namespace: String, otherApiAppName: String): TexasToken
}

data class TexasToken(val token: String)

class TexasCloudClient(
    @Named("RetryHttpClient") private val httpClient: HttpClient,
    private val env: Environment,
) : TexasClient {
    private val logger = logger()

    @WithSpan("Texas.requestToken")
    override suspend fun requestToken(
        @SpanAttribute("namespace") namespace: String,
        @SpanAttribute("API") otherApiAppName: String,
    ): TexasToken {
        val cluster = env.runtime.env.nais
        val target = "api://${cluster}.$namespace.$otherApiAppName/.default"
        val requestBody = TokenRequest(identity_provider = "entra_id", target = target)

        val response =
            httpClient.post(env.texas().tokenEndpoint) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

        if (!response.status.isSuccess()) {
            response.logNonSuccess(target)
            throw RuntimeException("Unable to request m2m token for: $target")
        }

        val body = response.body<TokenResponse>()
        return TexasToken(body.access_token)
    }

    private suspend fun HttpResponse.logNonSuccess(target: String) {
        if (this.contentType()?.isTextType() == true) {
            logger.error(
                "Unable to request m2m token for: ${target}, texas says: ${this.body<String>()}"
            )
        } else {
            logger.error(
                "Unable to request m2m token for: ${target}, texas responded with status ${this.status} and no content type"
            )
        }
    }

    internal data class TokenRequest(val identity_provider: String, val target: String)

    internal data class TokenResponse(
        val access_token: String,
        val expires_in: Int,
        val token_type: String,
    )
}
