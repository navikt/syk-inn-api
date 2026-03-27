package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr

import arrow.core.Either
import arrow.core.left
import arrow.core.right
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
import no.nav.tsm.core.logger
import no.nav.tsm.plugins.auth.TexasClient

sealed interface HprClient {

    enum class HprErrors {
        NotFound,
        UnknownError,
    }

    suspend fun getSykmelderByHpr(behandlerHpr: String): Either<HprErrors, SykmelderMedHpr>

    suspend fun getSykmelderByIdent(behandlerIdent: String): Either<HprErrors, SykmelderMedHpr>
}

class HprCloudClient(
    @Named("RetryHttpClient") httpClient: HttpClient,
    private val texasClient: TexasClient,
    private val environment: Environment,
) : HprClient {
    private val logger = logger()

    private val httpClient: HttpClient = httpClient.config {
        install(CallId) { intercept { request, callId -> request.header("Nav-CallId", callId) } }
    }

    override suspend fun getSykmelderByHpr(
        behandlerHpr: String
    ): Either<HprClient.HprErrors, SykmelderMedHpr> {
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
                mapHprSykmelderToSykmelderMedHpr(response.body()).right()
            }

            response.status == HttpStatusCode.NotFound -> {
                HprClient.HprErrors.NotFound.left()
            }

            else -> {
                logger.error(
                    "Unable to fetch sykmelder with hpr: $behandlerHpr, status: ${response.status}}"
                )

                HprClient.HprErrors.UnknownError.left()
            }
        }
    }

    override suspend fun getSykmelderByIdent(
        behandlerIdent: String
    ): Either<HprClient.HprErrors, SykmelderMedHpr> {
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
                mapHprSykmelderToSykmelderMedHpr(response.body()).right()
            }

            response.status == HttpStatusCode.NotFound -> {
                HprClient.HprErrors.NotFound.left()
            }

            else -> {
                logger.error(
                    "Unable to fetch sykmelder with ident <****** *****>. See teamlogger for more info. status: ${response.status}}"
                )

                HprClient.HprErrors.UnknownError.left()
            }
        }
    }

    private suspend fun getToken() =
        texasClient.requestToken("teamsykmelding", "syfohelsenettproxy")

    private fun mapHprSykmelderToSykmelderMedHpr(hprSykmelder: HprSykmelder): SykmelderMedHpr {
        requireNotNull(hprSykmelder.hprNummer) { "HprSykmelder må ha hprNummer" }

        val godkjenninger: List<SykmelderGodkjenning> =
            hprSykmelder.godkjenninger.map { godkjenning ->
                SykmelderGodkjenning(
                    autorisasjon = godkjenning.autorisasjon?.mapKodeverk(),
                    helsepersonellkategori = godkjenning.helsepersonellkategori?.mapKodeverk(),
                    tillegskompetanse =
                        godkjenning.tillegskompetanse?.map {
                            SykmelderTilleggskompetanse(
                                avsluttetStatus = it.avsluttetStatus?.mapKodeverk(),
                                gyldig =
                                    SykmelderPeriode(fra = it.gyldig?.fra, til = it.gyldig?.til),
                                type = it.type?.mapKodeverk(),
                            )
                        },
                )
            }

        return SykmelderMedHpr(
            ident = hprSykmelder.fnr,
            hprNummer = hprSykmelder.hprNummer,
            godkjenninger = godkjenninger,
            navn = hprSykmelder.toNavn(),
        )
    }

    private fun HprKodeverk.mapKodeverk(): SykmelderKodeverk {
        return SykmelderKodeverk(aktiv = this.aktiv, oid = this.oid, verdi = this.verdi)
    }

    private fun HprSykmelder.toNavn(): String {
        requireNotNull(fornavn) { "HprSykmelder må ha fornavn" }
        requireNotNull(etternavn) { "HprSykmelder må ha etternavn" }

        return listOfNotNull(fornavn, mellomnavn, etternavn).joinToString(" ")
    }
}
