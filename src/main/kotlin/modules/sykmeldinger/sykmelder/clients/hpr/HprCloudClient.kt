package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.callid.CallId
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.server.plugins.di.annotations.Named
import no.nav.tsm.core.Environment
import no.nav.tsm.plugins.auth.TexasClient

class HprException(message: String, cause: Exception?) : Exception(message, cause)

sealed interface HprClient {

    suspend fun getSykmelderByHpr(behandlerHpr: String): SykmelderMedHpr?

    suspend fun getSykmelderByIdent(behandlerIdent: String): SykmelderMedHpr?
}

class HprCloudClient(
    @Named("RetryHttpClient") httpClient: HttpClient,
    private val texasClient: TexasClient,
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

    private suspend fun getToken() =
        texasClient.requestToken("teamsykmelding", "syfohelsenettproxy")

    private fun mapHprSykmelderToSykmelderMedHpr(hprSykmelder: HprSykmelder): SykmelderMedHpr {
        requireNotNull(hprSykmelder.hprNummer, { "HprSykmelder må ha hprNummer" })
        requireNotNull(hprSykmelder.fornavn, { "HprSykmelder må ha fornavn" })
        requireNotNull(hprSykmelder.etternavn, { "HprSykmelder må ha etternavn" })

        return SykmelderMedHpr(
            ident = hprSykmelder.fnr,
            hprNummer = hprSykmelder.hprNummer,
            godkjenninger =
                hprSykmelder.godkjenninger.map { godkjenning ->
                    SykmelderGodkjenning(
                        autorisasjon = godkjenning.autorisasjon?.koddeverkkk(),
                        helsepersonellkategori = godkjenning.helsepersonellkategori?.koddeverkkk(),
                        tillegskompetanse =
                            godkjenning.tillegskompetanse?.map {
                                SykmelderTilleggskompetanse(
                                    avsluttetStatus = it.avsluttetStatus?.koddeverkkk(),
                                    gyldig =
                                        SykmelderPeriode(
                                            fra = it.gyldig?.fra,
                                            til = it.gyldig?.til,
                                        ),
                                    type = it.type?.koddeverkkk(),
                                )
                            },
                    )
                },
        )
    }

    private fun HprKode.koddeverkkk(): SykmelderKode {
        return SykmelderKode(aktiv = this.aktiv, oid = this.oid, verdi = this.verdi)
    }
}
