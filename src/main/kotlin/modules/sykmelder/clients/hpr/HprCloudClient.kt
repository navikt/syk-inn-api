package modules.sykmelder.clients.hpr

import core.Environment
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.callid.CallId
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.http.isSuccess
import io.ktor.server.plugins.di.annotations.Named
import modules.sykmelder.SykmelderMedHpr
import modules.sykmelder.clients.texas.TexasCloudClient

class HprException(message: String, cause: Exception?) : Exception(message, cause)

sealed interface HprClient {

    suspend fun getSykmelderByHpr(behandlerHpr: String): SykmelderMedHpr?

    suspend fun getSykmelderByIdent(behandlerIdent: String): SykmelderMedHpr?
}

class HprCloudClient(
    @Named("RetryHttpClient") httpClient: HttpClient,
    private val texasClient: TexasCloudClient,
    private val environment: Environment,
) : HprClient {
    private val httpClient: HttpClient =
        httpClient.config {
            install(CallId) {
                intercept { request, callId -> request.header("Nav-CallId", callId) }
            }
        }

    override suspend fun getSykmelderByHpr(behandlerHpr: String): SykmelderMedHpr? {
        val (accessToken) = getToken()

        val response =
            httpClient.get(
                "${environment.external().helsenettproxy}/api/v2/behandlerMedHprNummer"
            ) {
                headers {
                    append("Content-Type", "application/json")
                    append("HprNummer", behandlerHpr)
                    append("Authorization", "Bearer $accessToken")
                }
            }

        return when {
            response.status.isSuccess() -> {
                mapHprSykmelderToSykmelderMedHpr(response.body())
            }

            response.status == HttpStatusCode.NotFound -> null
            else -> {
                throw HprException(
                    "Unable to fetch sykmelder with hpr: $behandlerHpr, status: ${response.status}}",
                    null,
                )
            }
        }
    }

    override suspend fun getSykmelderByIdent(behandlerIdent: String): SykmelderMedHpr? {
        val (accessToken) = getToken()

        val response =
            httpClient.get("${environment.external().helsenettproxy}/api/v2/behandler") {
                headers {
                    append("Content-Type", "application/json")
                    append("behandlerFnr", behandlerIdent)
                    append("Authorization", "Bearer $accessToken")
                }
            }

        return when {
            response.status.isSuccess() -> {
                mapHprSykmelderToSykmelderMedHpr(response.body())
            }

            response.status == HttpStatusCode.NotFound -> null
            else -> {
                // TODO teamlog ident ved feil
                throw HprException(
                    "Unable to fetch sykmelder with ident <****** *****>. See teamlogger for more info. status: ${response.status}}",
                    null,
                )
            }
        }
    }

    suspend fun getToken() = texasClient.requestToken("teamsykmelding", "syfohelsenettproxy")

    fun mapHprSykmelderToSykmelderMedHpr(hprSykmelder: HprSykmelder): SykmelderMedHpr {
        requireNotNull(hprSykmelder.hprNummer, { "HprSykmelder må ha hprNummer" })
        requireNotNull(hprSykmelder.fornavn, { "HprSykmelder må ha fornavn" })
        requireNotNull(hprSykmelder.etternavn, { "HprSykmelder må ha etternavn" })

        return SykmelderMedHpr(
            ident = hprSykmelder.fnr,
            hprNummer = hprSykmelder.hprNummer,
            fornavn = hprSykmelder.fornavn,
            mellomnavn = hprSykmelder.mellomnavn,
            etternavn = hprSykmelder.etternavn,
        )
    }
}
