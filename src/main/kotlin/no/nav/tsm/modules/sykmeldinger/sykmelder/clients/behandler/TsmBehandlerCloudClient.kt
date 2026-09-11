package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.behandler

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
import no.nav.tsm.core.Environment
import no.nav.tsm.ktor.auth.texas.Texas
import no.nav.tsm.ktor.core.Navn
import no.nav.tsm.ktor.core.SimpleNavn
import no.nav.tsm.ktor.logger
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr.*

class TsmBehandlerCloudClient(
    @Named("RetryHttpClient") httpClient: HttpClient,
    private val texasClient: Texas,
    private val environment: Environment,
) : TsmBehandlerClient {
    private val logger = logger()

    private val httpClient: HttpClient = httpClient.config {
        install(CallId) { intercept { request, callId -> request.header("Nav-CallId", callId) } }
        install(ContentNegotiation) { jackson {} }
    }

    override suspend fun getSykmelderByHpr(
        behandlerHpr: String
    ): Either<TsmBehandlerClient.BehandlerErrors, SykmelderMedHpr> {
        val behandlerQuery = BehandlerQuery.HprQuery(behandlerHpr)
        return getSykmelder(behandlerQuery)
    }

    override suspend fun getSykmelderByIdent(
        fnr: String
    ): Either<TsmBehandlerClient.BehandlerErrors, SykmelderMedHpr> {
        val behandlerQuery = BehandlerQuery.FnrQuery(fnr)
        return getSykmelder(behandlerQuery)
    }

    private suspend fun getSykmelder(
        behandlerQuery: BehandlerQuery
    ): Either<TsmBehandlerClient.BehandlerErrors, SykmelderMedHpr> {
        val span = Span.current()
        val accessToken = getToken().token
        val response =
            httpClient.get("${environment.external().tsmBehandler}/api/behandler/search") {
                bearerAuth(accessToken)
                headers { append("Content-Type", "application/json") }
                setBody(behandlerQuery)
            }

        return when {
            response.status.isSuccess() -> {
                span.setAttribute("client.outcome", "ok")
                mapTsmBehandlerToSykmelderMedHpr(response.body()).right()
            }

            response.status == HttpStatusCode.NotFound -> {
                span.setAttribute("client.outcome", "not-found")
                TsmBehandlerClient.BehandlerErrors.NotFound.left()
            }

            else -> {
                span.setAttribute("client.outcome", response.status.toString())
                logger.error(
                    "Unable to fetch sykmelder with ident <****** *****>. See teamlogger for more info. status: ${response.status}}"
                )

                TsmBehandlerClient.BehandlerErrors.Unknown.left()
            }
        }
    }

    private suspend fun getToken() = texasClient.entraIdToken("tsm", "tsm-behandler")

    private fun mapTsmBehandlerToSykmelderMedHpr(behandler: TsmBehandler): SykmelderMedHpr {
        requireNotNull(behandler.hpr) { "HprSykmelder må ha hprNummer" }

        val godkjenninger: List<SykmelderGodkjenning> =
            behandler.godkjenning.map { godkjenning ->
                SykmelderGodkjenning(
                    autorisasjon = godkjenning.autorisasjon.mapKodeverk(),
                    helsepersonellkategori = godkjenning.helsepersonellkategori.mapKodeverk(),
                    tillegskompetanse =
                        godkjenning.tilleggskompetanse.map {
                            SykmelderTilleggskompetanse(
                                avsluttetStatus = it.avsluttetStatus.mapKodeverk(),
                                gyldig =
                                    SykmelderPeriode(
                                        fra = it.gyldig.fra.atStartOfDay(),
                                        til = it.gyldig.til?.atStartOfDay(),
                                    ),
                                type = it.type.mapKodeverk(),
                            )
                        },
                )
            }

        requireNotNull(behandler.navn)
        return SykmelderMedHpr(
            ident = behandler.ident,
            hprNummer = behandler.hpr,
            godkjenninger = godkjenninger,
            navn = behandler.navn.toNavn(),
            suspendert = behandler.suspendert,
        )
    }

    private fun TsmBehandlerKode.mapKodeverk(): SykmelderKode {
        return SykmelderKode(aktiv = true, oid = this.oid, verdi = this.verdi)
    }
}

private fun BehandlerNavn.toNavn(): Navn {
    return SimpleNavn(fornavn = fornavn, mellomnavn = mellomnavn, etternavn = etternavn)
}
