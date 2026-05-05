package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume

import arrow.core.getOrElse
import com.github.benmanes.caffeine.cache.Caffeine
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
            .expireAfterWrite(15.minutes.toJavaDuration())
            .build<String, Sykmelder>()

    private val pdlCaffeine =
        Caffeine.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(15.minutes.toJavaDuration())
            .build<String, PdlPerson>()

    /**
     * Scenarios:
     * - Has HPR but no FNR
     * - Has FNR but no HPR
     * - Has FNR and HPR (use HPR)
     */
    suspend fun getResourcesForSykmelding(record: SykmeldingRecord): RecordWithResources {
        val sykmelding = record.sykmelding
        if (sykmelding is Sykmelding.Utenlandsk) return RecordWithResources.Utenlandsk(record)

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

        val sykmelder =
            when {
                maybeHpr != null ->
                    sykmelderByHprCached(maybeHpr, sykmelding.metadata.genDate.toLocalDate())
                maybeIdent != null ->
                    sykmelderByIdentCached(maybeIdent, sykmelding.metadata.genDate.toLocalDate())
                else ->
                    error(
                        "Sykmelder in sykmelding of type ${record.sykmelding.type} without either ident or hpr. Impossible!!"
                    )
            }

        if (sykmelder !is Sykmelder.MedSuspensjon) {
            error("Sykmelder for sykmeldingId ${sykmelding.id} does not exist in HPR")
        }

        val person = pdlByIdentCached(sykmelder.ident)

        return RecordWithResources.Nasjonal(
            record = record,
            navn = person.navn,
            hpr = sykmelder.hpr,
            ident = sykmelder.ident,
        )
    }

    private suspend fun pdlByIdentCached(ident: String): PdlPerson {
        val existing = pdlCaffeine.getIfPresent(ident)
        if (existing != null) return existing

        val person =
            pdlClient.getPerson(ident).getOrElse {
                error("Unable to fetch person with ident $ident from pdlClient, cause: $it")
            }

        pdlCaffeine.put(ident, person)

        return person
    }

    private suspend fun sykmelderByHprCached(hpr: String, oppslagsdato: LocalDate): Sykmelder {
        val existing = hprCaffeine.getIfPresent(hpr)
        if (existing != null) return existing

        val sykmelder =
            sykmelderService.byHpr(hpr, oppslagsdato).getOrElse {
                error(
                    "Unable to fetch sykmelder with hpr $hpr from sykmelderService, cause: ${it.details}"
                )
            }

        hprCaffeine.put(hpr, sykmelder)

        return sykmelder
    }

    private suspend fun sykmelderByIdentCached(ident: String, oppslagsdato: LocalDate): Sykmelder {
        val existing = hprCaffeine.getIfPresent(ident)
        if (existing != null) return existing

        val sykmelder =
            sykmelderService.byIdent(ident, oppslagsdato).getOrElse {
                error(
                    "Unable to fetch sykmelder with ident $ident from sykmelderService, cause: $it"
                )
            }

        hprCaffeine.put(ident, sykmelder)

        return sykmelder
    }
}
