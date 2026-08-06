package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.callid.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson3.*
import io.ktor.server.plugins.di.annotations.*
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.tsm.core.Environment
import no.nav.tsm.core.common.Navn
import no.nav.tsm.core.common.SimpleNavn
import no.nav.tsm.ktor.auth.texas.Texas
import no.nav.tsm.ktor.logger

class HprCloudClient(
    @Named("RetryHttpClient") httpClient: HttpClient,
    private val texasClient: Texas,
    private val environment: Environment,
) : HprClient {
    private val logger = logger()

    private val httpClient: HttpClient = httpClient.config {
        install(CallId) { intercept { request, callId -> request.header("Nav-CallId", callId) } }
        install(ContentNegotiation) { jackson {} }
    }

    @WithSpan
    override suspend fun getSykmelderByHpr(
        behandlerHpr: String
    ): Either<HprClient.HprErrors, SykmelderMedHpr> {
        val span = Span.current()
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
                span.setAttribute("client.outcome", "ok")
                mapHprSykmelderToSykmelderMedHpr(response.body()).right()
            }

            response.status == HttpStatusCode.NotFound -> {
                span.setAttribute("client.outcome", "not-found")
                HprClient.HprErrors.NotFound.left()
            }

            else -> {
                span.setAttribute("client.outcome", response.status.toString())
                logger.error(
                    "Unable to fetch sykmelder with hpr: $behandlerHpr, status: ${response.status}}"
                )

                HprClient.HprErrors.UnknownError.left()
            }
        }
    }

    @WithSpan
    override suspend fun getSykmelderByIdent(
        behandlerIdent: String
    ): Either<HprClient.HprErrors, SykmelderMedHpr> {
        val span = Span.current()
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
                span.setAttribute("client.outcome", "ok")
                mapHprSykmelderToSykmelderMedHpr(response.body()).right()
            }

            response.status == HttpStatusCode.NotFound -> {
                span.setAttribute("client.outcome", "not-found")
                HprClient.HprErrors.NotFound.left()
            }

            else -> {
                span.setAttribute("client.outcome", response.status.toString())
                logger.error(
                    "Unable to fetch sykmelder with ident <****** *****>. See teamlogger for more info. status: ${response.status}}"
                )

                HprClient.HprErrors.UnknownError.left()
            }
        }
    }

    private suspend fun getToken() =
        texasClient.entraIdToken("teamsykmelding", "syfohelsenettproxy")

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

    private fun HprKodeverk.mapKodeverk(): SykmelderKode {
        return SykmelderKode(aktiv = this.aktiv, oid = this.oid, verdi = this.verdi)
    }

    private fun HprSykmelder.toNavn(): Navn {
        requireNotNull(fornavn) { "HprSykmelder må ha fornavn" }
        requireNotNull(etternavn) { "HprSykmelder må ha etternavn" }

        return SimpleNavn(fornavn = fornavn, mellomnavn = mellomnavn, etternavn = etternavn)
    }
}
