package no.nav.tsm.modules.sykmeldinger.pdl

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.*
import io.ktor.serialization.jackson.jackson
import io.ktor.server.plugins.di.annotations.*
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.tsm.core.Environment
import no.nav.tsm.core.logger
import no.nav.tsm.core.otel.failSpan
import no.nav.tsm.plugins.auth.TexasClient

sealed interface PdlClient {
    enum class PdlErrors {
        NotFound,
        UnknownError,
    }

    suspend fun getPerson(ident: String): Either<PdlErrors, PdlPerson>
}

class PdlCloudClient(
    @Named("RetryHttpClient") httpClient: HttpClient,
    private val texasClient: TexasClient,
    private val environment: Environment,
) : PdlClient {
    private val logger = logger()

    private val pdlHttpClient = httpClient.config {
        install(ContentNegotiation) { jackson { registerModule(JavaTimeModule()) } }
    }

    @WithSpan
    override suspend fun getPerson(ident: String): Either<PdlClient.PdlErrors, PdlPerson> {
        val (token) = getToken()

        val response =
            pdlHttpClient.get("${environment.external().tsmPdlCache}/api/person") {
                headers {
                    append("Nav-Consumer-Id", "syk-inn-api")
                    append("Authorization", "Bearer $token")
                    append("Ident", ident)
                }
            }

        val body: PdlPerson =
            try {
                response.body<PdlPerson>()
            } catch (e: Exception) {
                failSpan(Span.current(), e)
                logger.error("Error deserializing PDL response", e)
                return PdlClient.PdlErrors.UnknownError.left()
            }

        return when {
            response.status.isSuccess() -> body.right()
            response.status == HttpStatusCode.NotFound -> PdlClient.PdlErrors.NotFound.left()
            else -> {
                logger.error("Unable to get person from pdl, see team logs for ident")
                PdlClient.PdlErrors.UnknownError.left()
            }
        }
    }

    private suspend fun getToken() = texasClient.requestToken("tsm", "tsm-pdl-cache")
}
