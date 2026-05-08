package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import com.github.benmanes.caffeine.cache.Caffeine
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration
import no.nav.tsm.core.common.Navn
import no.nav.tsm.modules.sykmeldinger.pdl.PdlClient
import no.nav.tsm.modules.sykmeldinger.pdl.PdlPerson
import no.nav.tsm.modules.sykmeldinger.sykmelder.Sykmelder
import no.nav.tsm.modules.sykmeldinger.sykmelder.SykmelderService
import no.nav.tsm.sykmelding.input.core.model.Sykmelding
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType

sealed interface RecordWithResources {
    val record: SykmeldingRecord

    data class Nasjonal(
        override val record: SykmeldingRecord,
        val navn: Navn,
        val hpr: String,
        val ident: String,
    ) : RecordWithResources

    data class Utenlandsk(override val record: SykmeldingRecord) : RecordWithResources
}

sealed interface RecordResourceErrors {
    val skippableInDev: Boolean

    object SykmelderWithoutIdent : RecordResourceErrors {
        override val skippableInDev: Boolean = false
    }

    object SykmelderPdlNotFound : RecordResourceErrors {
        override val skippableInDev: Boolean = true
    }

    object SykmelderHprNotFound : RecordResourceErrors {
        override val skippableInDev: Boolean = true
    }

    object SykmelderSuspendertNotFound : RecordResourceErrors {
        override val skippableInDev: Boolean = true
    }

    data class AnyUnknownError(val where: String, val cause: String) : RecordResourceErrors {
        override val skippableInDev: Boolean = false
    }
}

/**
 * This service has aggressive caching for PDL and HPR mostly useful for reading large amounts of
 * records from kafka.
 */
class SykmeldingConsumerResourcesService(
    private val pdlClient: PdlClient,
    private val sykmelderService: SykmelderService,
) {
    private val hprCaffeine =
        Caffeine.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(60.minutes.toJavaDuration())
            .build<String, Sykmelder>()

    private val pdlCaffeine =
        Caffeine.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(60.minutes.toJavaDuration())
            .build<String, PdlPerson>()

    /**
     * Scenarios:
     * - Has HPR but no FNR
     * - Has FNR but no HPR
     * - Has FNR and HPR (use HPR)
     */
    @WithSpan
    suspend fun getResourcesForSykmelding(
        record: SykmeldingRecord
    ): Either<RecordResourceErrors, RecordWithResources> = either {
        val span = Span.current()
        span.setAttribute("sykmelding.id", record.sykmelding.id)

        val sykmelding = record.sykmelding
        if (sykmelding is Sykmelding.Utenlandsk)
            return RecordWithResources.Utenlandsk(record).right()

        val sykmelderInSykmelding =
            when (sykmelding) {
                is Sykmelding.Digital -> sykmelding.sykmelder
                is Sykmelding.Papir -> sykmelding.sykmelder
                is Sykmelding.Xml -> sykmelding.sykmelder
            }

        val maybeHpr: String? =
            sykmelderInSykmelding.ids.firstOrNull { it.type == PersonIdType.HPR }?.id
        val maybeIdent: String? =
            sykmelderInSykmelding.ids
                .firstOrNull { it.type in listOf(PersonIdType.FNR, PersonIdType.DNR) }
                ?.id

        val sykmelder: Sykmelder =
            when {
                maybeHpr != null ->
                    sykmelderByHprCached(maybeHpr, sykmelding.metadata.genDate.toLocalDate())

                maybeIdent != null ->
                    sykmelderByIdentCached(maybeIdent, sykmelding.metadata.genDate.toLocalDate())

                else -> raise(RecordResourceErrors.SykmelderWithoutIdent)
            }.bind()

        if (sykmelder !is Sykmelder.MedSuspensjon) {
            span.setAttribute("resource.outcome", "sykmelder-not-found")
            raise(RecordResourceErrors.SykmelderHprNotFound)
        }

        val person = pdlByIdentCached(sykmelder.ident).bind()
        if (person.navn == null) {
            span.setAttribute("resource.outcome", "sykmelder-without-name")
            raise(RecordResourceErrors.SykmelderPdlNotFound)
        }

        return RecordWithResources.Nasjonal(
                record = record,
                navn = person.navn,
                hpr = sykmelder.hpr,
                ident = sykmelder.ident,
            )
            .right()
    }

    @WithSpan
    private suspend fun pdlByIdentCached(ident: String): Either<RecordResourceErrors, PdlPerson> =
        either {
            val span = Span.current()

            val existing = pdlCaffeine.getIfPresent(ident)
            if (existing != null) {
                span.setAttribute("cache.hit", "true")
                return existing.right()
            }

            val person =
                pdlClient
                    .getPerson(ident)
                    .mapLeft {
                        span.setAttribute("resource.outcome", it.name)
                        when (it) {
                            PdlClient.PdlErrors.NotFound ->
                                RecordResourceErrors.SykmelderPdlNotFound
                            PdlClient.PdlErrors.UnknownError ->
                                RecordResourceErrors.AnyUnknownError(
                                    "PdlClient",
                                    "Unknown PDL error, client has logged exception",
                                )
                        }
                    }
                    .bind()

            pdlCaffeine.put(ident, person)
            span.setAttribute("cache.hit", "false")

            return person.right()
        }

    @WithSpan
    private suspend fun sykmelderByHprCached(
        hpr: String,
        oppslagsdato: LocalDate,
    ): Either<RecordResourceErrors, Sykmelder> = either {
        val span = Span.current()

        val existing = hprCaffeine.getIfPresent(hpr)
        if (existing != null) {
            span.setAttribute("cache.hit", "true")
            return existing.right()
        }

        val sykmelder =
            sykmelderService.byHpr(hpr, oppslagsdato).mapLeft { it.toRecordResourcError() }.bind()

        hprCaffeine.put(hpr, sykmelder)
        span.setAttribute("cache.hit", "false")

        return sykmelder.right()
    }

    @WithSpan
    private suspend fun sykmelderByIdentCached(
        ident: String,
        oppslagsdato: LocalDate,
    ): Either<RecordResourceErrors, Sykmelder> = either {
        val span = Span.current()

        val existing = hprCaffeine.getIfPresent(ident)
        if (existing != null) {
            span.setAttribute("cache.hit", "true")
            return existing.right()
        }

        val sykmelder =
            sykmelderService
                .byIdent(ident, oppslagsdato)
                .mapLeft { it.toRecordResourcError() }
                .bind()

        hprCaffeine.put(ident, sykmelder)
        span.setAttribute("cache.hit", "false")

        return sykmelder.right()
    }

    private fun SykmelderService.SykmelderErrors.toRecordResourcError(): RecordResourceErrors {
        Span.current().setAttribute("resource.outcome", this.name)

        return when (this) {
            SykmelderService.SykmelderErrors.HprUnknownError ->
                RecordResourceErrors.SykmelderHprNotFound

            SykmelderService.SykmelderErrors.SuspendertNotFound ->
                RecordResourceErrors.SykmelderSuspendertNotFound

            SykmelderService.SykmelderErrors.SuspendertUnknownError ->
                RecordResourceErrors.AnyUnknownError(
                    "SykmelderService",
                    "Unknown error, service has logged exception",
                )
        }
    }
}
