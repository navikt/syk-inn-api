package no.nav.tsm.syk_inn_api.model.sykmelding.kafka

import java.time.LocalDate
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
    val sykmeldingRecordMedisinskVurdering: SykmeldingRecordMedisinskVurdering
    val sykmeldingRecordAktivitet: List<SykmeldingRecordAktivitet>
}

data class DigitalSykmelding(
    override val id: String,
    override val metadata: DigitalSykmeldingMetadata,
    override val pasient: Pasient,
    override val sykmeldingRecordMedisinskVurdering: SykmeldingRecordMedisinskVurdering,
    override val sykmeldingRecordAktivitet: List<SykmeldingRecordAktivitet>,
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
    XML,
    PAPIR,
    UTENLANDSK
}

sealed interface SykmeldingMeta {
    val mottattDato: OffsetDateTime
    val genDate: OffsetDateTime
}

data class DigitalSykmeldingMetadata(
    override val mottattDato: OffsetDateTime,
    override val genDate: OffsetDateTime,
) : SykmeldingMeta

data class OldSykmeldingMetadata(
    override val mottattDato: OffsetDateTime,
    override val genDate: OffsetDateTime,
    val behandletTidspunkt: OffsetDateTime,
    val regelsettVersjon: String?,
    val avsenderSystem: AvsenderSystem,
    val strekkode: String?,
) : SykmeldingMeta

data class XmlSykmelding(
    override val id: String,
    override val metadata: OldSykmeldingMetadata,
    override val pasient: Pasient,
    override val sykmeldingRecordMedisinskVurdering: SykmeldingRecordMedisinskVurdering,
    override val sykmeldingRecordAktivitet: List<SykmeldingRecordAktivitet>,
    val arbeidsgiver: ArbeidsgiverInfo,
    val behandler: Behandler,
    val sykmelder: Sykmelder,
    val prognose: Prognose?,
    val tiltak: Tiltak?,
    val bistandNav: BistandNav?,
    val tilbakedatering: Tilbakedatering?,
    val utdypendeOpplysninger: Map<String, Map<String, SporsmalSvar>>?,
) : ISykmelding {
    override val type = SykmeldingType.XML
}

data class Papirsykmelding(
    override val id: String,
    override val metadata: OldSykmeldingMetadata,
    override val pasient: Pasient,
    override val sykmeldingRecordMedisinskVurdering: SykmeldingRecordMedisinskVurdering,
    override val sykmeldingRecordAktivitet: List<SykmeldingRecordAktivitet>,
    val arbeidsgiver: ArbeidsgiverInfo,
    val behandler: Behandler,
    val sykmelder: Sykmelder,
    val prognose: Prognose?,
    val tiltak: Tiltak?,
    val bistandNav: BistandNav?,
    val tilbakedatering: Tilbakedatering?,
    val utdypendeOpplysninger: Map<String, Map<String, SporsmalSvar>>?,
) : ISykmelding {
    override val type = SykmeldingType.PAPIR
}

data class UtenlandskSykmelding(
    override val id: String,
    override val metadata: OldSykmeldingMetadata,
    override val pasient: Pasient,
    override val sykmeldingRecordMedisinskVurdering: SykmeldingRecordMedisinskVurdering,
    override val sykmeldingRecordAktivitet: List<SykmeldingRecordAktivitet>,
    val utenlandskInfo: UtenlandskInfo
) : ISykmelding {
    override val type = SykmeldingType.UTENLANDSK
}

data class BistandNav(
    val bistandUmiddelbart: Boolean,
    val beskrivBistand: String?,
)

data class Tiltak(
    val tiltakNav: String?,
    val andreTiltak: String?,
)

data class Prognose(
    val arbeidsforEtterPeriode: Boolean,
    val hensynArbeidsplassen: String?,
    val arbeid: IArbeid?,
)

data class Tilbakedatering(
    val kontaktDato: LocalDate?,
    val begrunnelse: String?,
)

data class UtenlandskInfo(
    val land: String,
    val folkeRegistertAdresseErBrakkeEllerTilsvarende: Boolean,
    val erAdresseUtland: Boolean?,
)

data class SporsmalSvar(
    val sporsmal: String?,
    val svar: String,
    val restriksjoner: List<SvarRestriksjon>
)

enum class SvarRestriksjon {
    SKJERMET_FOR_ARBEIDSGIVER,
    SKJERMET_FOR_PASIENT,
    SKJERMET_FOR_NAV,
}

data class AvsenderSystem(val navn: String, val versjon: String)
