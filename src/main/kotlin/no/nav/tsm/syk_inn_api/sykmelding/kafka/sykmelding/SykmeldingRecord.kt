package no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding

import java.time.LocalDate
import java.time.OffsetDateTime
import no.nav.tsm.syk_inn_api.common.Navn
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.HelsepersonellKategori
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.KafkaPersonNavn
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.Kontaktinfo
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.MessageMetadata
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.PersonId
import no.nav.tsm.syk_inn_api.sykmelding.rules.ValidationResult

data class SykmeldingRecord(
    val metadata: MessageMetadata,
    val sykmelding: ISykmelding,
    val validation: ValidationResult,
)

sealed interface ISykmelding {
    val type: SykmeldingType
    val id: String
    val metadata: SykmeldingMeta
    val pasient: SykmeldingRecordPasient
    val medisinskVurdering: SykmeldingRecordMedisinskVurdering
    val aktivitet: List<SykmeldingRecordAktivitet>
}

data class DigitalSykmelding(
    override val id: String,
    override val metadata: DigitalSykmeldingMetadata,
    override val pasient: SykmeldingRecordPasient,
    override val medisinskVurdering: SykmeldingRecordMedisinskVurdering,
    override val aktivitet: List<SykmeldingRecordAktivitet>,
    val behandler: SykmeldingRecordBehandler,
    val sykmelder: SykmeldingRecordSykmelder,
    val arbeidsgiver: SykmeldingRecordArbeidsgiverInfo?,
    val tilbakedatering: SykmeldingRecordTilbakedatering?,
    val meldinger: SykmeldingRecordMeldinger,
) : ISykmelding {
    override val type = SykmeldingType.DIGITAL
}

data class SykmeldingRecordMeldinger(
    val tilNav: String?,
    val tilArbeidsgiver: String?,
)

data class SykmeldingRecordPasient(
    val navn: Navn?,
    val fnr: String,
    val kontaktinfo: List<Kontaktinfo>,
)

data class SykmeldingRecordBehandler(
    val navn: KafkaPersonNavn,
    val ids: List<PersonId>,
    val kontaktinfo: List<Kontaktinfo>,
)

data class SykmeldingRecordSykmelder(
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
    override val pasient: SykmeldingRecordPasient,
    override val medisinskVurdering: SykmeldingRecordMedisinskVurdering,
    override val aktivitet: List<SykmeldingRecordAktivitet>,
    val arbeidsgiver: SykmeldingRecordArbeidsgiverInfo,
    val behandler: SykmeldingRecordBehandler,
    val sykmelder: SykmeldingRecordSykmelder,
    val prognose: SykmeldingRecordPrognose?,
    val tiltak: SykmeldingRecordTiltak?,
    val bistandNav: SykmeldingRecordBistandNav?,
    val tilbakedatering: SykmeldingRecordTilbakedatering?,
    val utdypendeOpplysninger: Map<String, Map<String, SporsmalSvar>>?,
) : ISykmelding {
    override val type = SykmeldingType.XML
}

data class Papirsykmelding(
    override val id: String,
    override val metadata: OldSykmeldingMetadata,
    override val pasient: SykmeldingRecordPasient,
    override val medisinskVurdering: SykmeldingRecordMedisinskVurdering,
    override val aktivitet: List<SykmeldingRecordAktivitet>,
    val arbeidsgiver: SykmeldingRecordArbeidsgiverInfo,
    val behandler: SykmeldingRecordBehandler,
    val sykmelder: SykmeldingRecordSykmelder,
    val prognose: SykmeldingRecordPrognose?,
    val tiltak: SykmeldingRecordTiltak?,
    val bistandNav: SykmeldingRecordBistandNav?,
    val tilbakedatering: SykmeldingRecordTilbakedatering?,
    val utdypendeOpplysninger: Map<String, Map<String, SporsmalSvar>>?,
) : ISykmelding {
    override val type = SykmeldingType.PAPIR
}

data class UtenlandskSykmelding(
    override val id: String,
    override val metadata: OldSykmeldingMetadata,
    override val pasient: SykmeldingRecordPasient,
    override val medisinskVurdering: SykmeldingRecordMedisinskVurdering,
    override val aktivitet: List<SykmeldingRecordAktivitet>,
    val utenlandskInfo: UtenlandskInfo
) : ISykmelding {
    override val type = SykmeldingType.UTENLANDSK
}

data class SykmeldingRecordBistandNav(
    val bistandUmiddelbart: Boolean,
    val beskrivBistand: String?,
)

data class SykmeldingRecordTiltak(
    val tiltakNav: String?,
    val andreTiltak: String?,
)

data class SykmeldingRecordPrognose(
    val arbeidsforEtterPeriode: Boolean,
    val hensynArbeidsplassen: String?,
    val arbeid: IArbeid?,
)

data class SykmeldingRecordTilbakedatering(
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
