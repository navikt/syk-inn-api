package modules.external.clients.texas

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
import no.nav.tsm.core.Environment
import no.nav.tsm.core.RuntimeEnvironments
import no.nav.tsm.core.logger

data class TexasToken(val token: String)

enum class TexasCluster(val nais: String) {
    Prod("prod-gcp"),
    Dev("dev-gcp"),
}

class TexasClient(
    @Named("RetryHttpClient") private val httpClient: HttpClient,
    private val env: Environment,
) {
    private val logger = logger()

    @WithSpan("Texas.requestToken")
    suspend fun requestToken(
        @SpanAttribute("namespace") namespace: String,
        @SpanAttribute("API") otherApiAppName: String,
    ): TexasToken {
        val cluster = env.runtimeEnv.toCluster().nais
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

    private fun RuntimeEnvironments.toCluster(): TexasCluster {
        return when (this) {
            RuntimeEnvironments.PROD -> TexasCluster.Prod
            RuntimeEnvironments.DEV -> TexasCluster.Dev
            RuntimeEnvironments.LOCAL ->
                throw IllegalStateException(
                    "Local environment should not request tokens from texas."
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
