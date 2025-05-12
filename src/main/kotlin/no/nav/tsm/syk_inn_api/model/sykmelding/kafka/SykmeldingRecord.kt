package no.nav.tsm.syk_inn_api.model.sykmelding.kafka

import java.time.OffsetDateTime
import no.nav.tsm.mottak.sykmelding.model.metadata.HelsepersonellKategori
import no.nav.tsm.mottak.sykmelding.model.metadata.Kontaktinfo
import no.nav.tsm.mottak.sykmelding.model.metadata.MessageMetadata
import no.nav.tsm.mottak.sykmelding.model.metadata.PersonId
import no.nav.tsm.syk_inn_api.model.Navn
import no.nav.tsm.syk_inn_api.model.ValidationResult

data class SykmeldingRecord(
    val metadata: MessageMetadata,
    val sykmelding: ISykmelding,
    val validation: ValidationResult,
)

sealed interface ISykmelding {
    val type: SykmeldingType
    val id: String
    val metadata: SykmeldingMeta
    val pasient: Pasient
    val medisinskVurdering: MedisinskVurdering
    val aktivitetKafka: List<AktivitetKafka>
}

data class SykInnSykmelding(
    override val id: String,
    override val metadata: DigitalSykmeldingMetadata,
    override val pasient: Pasient,
    override val medisinskVurdering: MedisinskVurdering,
    override val aktivitetKafka: List<AktivitetKafka>,
    val behandler: Behandler,
    val sykmelder: Sykmelder,
) : ISykmelding {
    override val type = SykmeldingType.DIGITAL
}

data class Pasient(
    val navn: Navn?,
    val fnr: String,
    val kontaktinfo: List<Kontaktinfo>,
)

data class Behandler(
    val navn: no.nav.tsm.mottak.sykmelding.model.metadata.Navn,
    //    val adresse: Adresse?,
    val ids: List<PersonId>,
    val kontaktinfo: List<Kontaktinfo>,
)

data class Sykmelder(
    val ids: List<PersonId>,
    val helsepersonellKategori: HelsepersonellKategori,
)

enum class SykmeldingType {
    DIGITAL,
}

sealed interface SykmeldingMeta {
    val mottattDato: OffsetDateTime
    val genDate: OffsetDateTime
}

data class DigitalSykmeldingMetadata(
    override val mottattDato: OffsetDateTime,
    override val genDate: OffsetDateTime,
) : SykmeldingMeta
