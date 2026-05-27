package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.jackson
import io.ktor.server.plugins.di.annotations.Named
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.tsm.core.Environment
import no.nav.tsm.core.common.Navn
import no.nav.tsm.core.common.SimpleNavn
import no.nav.tsm.core.logger
import no.nav.tsm.plugins.auth.TexasClient

class HprRestCloudClient(
    @Named("RetryHttpClient") httpClient: HttpClient,
    private val texasClient: TexasClient,
    private val environment: Environment,
) : HprClient {
    private val logger = logger()

    private val httpClient: HttpClient = httpClient.config {
        defaultRequest { url(environment.external().hpr) }

        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
    }

    @WithSpan
    override suspend fun getSykmelderByHpr(
        behandlerHpr: String
    ): Either<HprClient.HprErrors, SykmelderMedHpr> {
        val span = Span.current()
        val (accessToken) = getToken()

        val response =
            httpClient.get("/v1/personerutvidet/$behandlerHpr") { bearerAuth(accessToken) }

        return when {
            response.status.isSuccess() -> {
                span.setAttribute("client.outcome", "ok")

                response.body<HprResponse.HelsepersonUtvidet>().toBehandler().right()
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
            httpClient.post("/v1/personerutvidet") {
                bearerAuth(accessToken)
                contentType(ContentType.Application.Json)
                setBody(HprRequest.ByIdent(nin = behandlerIdent, atDate = LocalDate.now()))
            }

        return when {
            response.status.isSuccess() -> {
                span.setAttribute("client.outcome", "ok")

                response.body<HprResponse.HelsepersonUtvidet>().toBehandler().right()
            }

            response.status == HttpStatusCode.NotFound -> {
                span.setAttribute("client.outcome", "not-found")

                HprClient.HprErrors.NotFound.left()
            }

            else -> {
                span.setStatus(StatusCode.ERROR)
                span.setAttribute("client.outcome", response.status.toString())
                logger.error("Unable to fetch sykmelder by ident, status: ${response.status}}")

                HprClient.HprErrors.UnknownError.left()
            }
        }
    }

    private suspend fun getToken() = texasClient.maskinporten("nhn:hpr/basic nhn:hpr/extended")

    private fun HprResponse.HelsepersonUtvidet.toBehandler(): SykmelderMedHpr =
        SykmelderMedHpr(
            godkjenninger = godkjenninger?.map { it.toGodkjenning() } ?: emptyList(),
            ident = requireNotNull(person?.nin) { "HprSykmelder må ha fnr (nin) i HPR" },
            hprNummer = hprNummer.toString(),
            navn = person.toNavn(),
        )

    private fun HprResponse.Godkjenning.toGodkjenning(): SykmelderGodkjenning =
        SykmelderGodkjenning(
            helsepersonellkategori = helsepersonellkategori?.toKode(),
            autorisasjon = autorisasjon?.toKode(),
            tillegskompetanse = tilleggskompetanser?.map { it.toTilleggskompetanse() },
        )

    private fun HprResponse.Kode.toKode(): SykmelderKode =
        SykmelderKode(aktiv = true, oid = kodeverk?.id ?: 0, verdi = verdi)

    private fun HprResponse.Tilleggskompetanse.toTilleggskompetanse(): SykmelderTilleggskompetanse =
        SykmelderTilleggskompetanse(
            avsluttetStatus = avsluttetStatus?.toKode(),
            gyldig = periode?.toPeriode(),
            type = type?.toKode(),
        )

    private fun HprResponse.Periode.toPeriode(): SykmelderPeriode =
        SykmelderPeriode(fra = fra?.atStartOfDay(), til = til?.atStartOfDay())

    private fun HprResponse.PersonUtvidet.toNavn(): Navn {
        requireNotNull(fornavn) { "HprSykmelder må ha fornavn" }
        requireNotNull(etternavn) { "HprSykmelder må ha etternavn" }

        return SimpleNavn(fornavn = fornavn, mellomnavn = mellomnavn, etternavn = etternavn)
    }
}

object HprRequest {
    data class ByIdent(
        /** nin: National Identification Number, i.e. fødselsnummer or D-nummer (?) */
        val nin: String,
        val atDate: LocalDate? = null,
        val includeHistory: Boolean? = null,
    )
}

/**
 * These are the classes used to destructure the responses from HPR.
 *
 * These should never be used outside of this client.
 */
object HprResponse {
    data class HelsepersonUtvidet(
        val person: PersonUtvidet? = null,
        val hprNummer: Int,
        val erstattetAvHprNummer: Int? = null,
        val godkjenninger: List<Godkjenning>? = null,
        val administrativeReaksjoner: List<AdministrativReaksjon>? = null,
        val sistOppdatert: LocalDateTime? = null,
        val gjelderForDato: LocalDate? = null,
    )

    data class PersonUtvidet(
        val fornavn: String? = null,
        val mellomnavn: String? = null,
        val etternavn: String? = null,
        @param:JsonProperty("fødselsdato") val fodselsdato: LocalDate? = null,
        @param:JsonProperty("dødsdato") val dodsdato: LocalDate? = null,
        val manglerNin: Boolean = false,
        val nin: String? = null,
    )

    data class Godkjenning(
        val helsepersonellkategori: Kode? = null,
        val autorisasjon: Kode? = null,
        val periode: Periode? = null,
        val godkjentTurnusDato: LocalDate? = null,
        val rekvisisjonsretter: List<Rekvisisjonsrett>? = null,
        val spesialistgodkjenninger: List<Spesialistgodkjenning>? = null,
        val tilleggskompetanser: List<Tilleggskompetanse>? = null,
        @param:JsonProperty("vilkår") val vilkar: List<Vilkar>? = null,
        val avsluttetStatus: Kode? = null,
        val administrativeReaksjoner: List<AdministrativReaksjon>? = null,
    )

    data class Kode(
        val navn: String? = null,
        val verdi: String? = null,
        val kodeverk: Kodeverk? = null,
    )

    data class Kodeverk(val id: Int? = null, val navn: String? = null)

    data class Periode(val fra: LocalDate? = null, val til: LocalDate? = null)

    data class Rekvisisjonsrett(
        val type: Kode? = null,
        val periode: Periode? = null,
        val avsluttetStatus: Kode? = null,
        val administrativeReaksjoner: List<AdministrativReaksjon>? = null,
    )

    data class Spesialistgodkjenning(
        val type: Kode? = null,
        val periode: Periode? = null,
        val avsluttetStatus: Kode? = null,
        val administrativeReaksjoner: List<AdministrativReaksjon>? = null,
    )

    data class Tilleggskompetanse(
        val type: Kode? = null,
        val periode: Periode? = null,
        val avsluttetStatus: Kode? = null,
    )

    data class Vilkar(val type: Kode? = null)

    data class AdministrativReaksjon(val type: Kode? = null, val periode: Periode? = null)
}
