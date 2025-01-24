package no.nav.tsm.sykinnapi.modell.syfosmregister

import java.time.LocalDate
import java.time.OffsetDateTime

data class SykmeldingDTO(
    val id: String,
    val mottattTidspunkt: OffsetDateTime,
    val behandlingsutfall: BehandlingsutfallDTO,
    val legekontorOrgnummer: String?,
    val arbeidsgiver: ArbeidsgiverDTO?,
    val sykmeldingsperioder: List<SykmeldingsperiodeDTO>,
    val sykmeldingStatus: SykmeldingStatusDTO,
    val medisinskVurdering: MedisinskVurderingDTO?,
    val skjermesForPasient: Boolean,
    val prognose: PrognoseDTO?,
    val utdypendeOpplysninger: Map<String, Map<String, SporsmalSvarDTO>>,
    val tiltakArbeidsplassen: String?,
    val tiltakNAV: String?,
    val andreTiltak: String?,
    val meldingTilNAV: MeldingTilNavDTO?,
    val meldingTilArbeidsgiver: String?,
    val kontaktMedPasient: KontaktMedPasientDTO,
    val behandletTidspunkt: OffsetDateTime,
    val behandler: BehandlerDTO,
    val syketilfelleStartDato: LocalDate?,
    val navnFastlege: String?,
    val egenmeldt: Boolean?,
    val papirsykmelding: Boolean?,
    val harRedusertArbeidsgiverperiode: Boolean?,
    val merknader: List<MerknadDTO>?,
    val utenlandskSykmelding: UtenlandskSykmeldingDTO?,
)
