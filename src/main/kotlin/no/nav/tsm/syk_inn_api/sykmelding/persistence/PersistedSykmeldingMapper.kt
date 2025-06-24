package no.nav.tsm.syk_inn_api.sykmelding.persistence

import java.time.LocalDate
import java.time.Month
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.common.DiagnosekodeMapper
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.sykmelder.Sykmelder
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprGodkjenning
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprGyldighetsPeriode
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprKode
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprTilleggskompetanse
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingArbeidsgiver
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingDiagnoseInfo
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingMeldinger
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingPayload
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingTilbakedatering
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingYrkesskade
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.Digital
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.EDIEmottak
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.Egenmeldt
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.EmottakEnkel
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.KontaktinfoType
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.Papir
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.Utenlandsk
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.DigitalSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.Papirsykmelding
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecord
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.XmlSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.rules.RuleType
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.sykmelding.input.core.model.Aktivitet
import no.nav.tsm.sykmelding.input.core.model.AktivitetIkkeMulig
import no.nav.tsm.sykmelding.input.core.model.ArbeidsgiverInfo
import no.nav.tsm.sykmelding.input.core.model.Avventende
import no.nav.tsm.sykmelding.input.core.model.Behandlingsdager
import no.nav.tsm.sykmelding.input.core.model.DiagnoseInfo
import no.nav.tsm.sykmelding.input.core.model.Gradert
import no.nav.tsm.sykmelding.input.core.model.MedisinskVurdering
import no.nav.tsm.sykmelding.input.core.model.Pasient
import no.nav.tsm.sykmelding.input.core.model.Reisetilskudd
import no.nav.tsm.sykmelding.input.core.model.Tilbakedatering

object PersistedSykmeldingMapper {

    private val logger = logger()

    fun mapSykmeldingPayloadToPersistedSykmelding(
        payload: OpprettSykmeldingPayload,
        sykmeldingId: String,
        pasient: Person,
        sykmelder: Sykmelder,
        ruleResult: RegulaResult
    ): PersistedSykmelding {
        return PersistedSykmelding(
            sykmeldingId = sykmeldingId,
            hoveddiagnose = payload.values.hoveddiagnose.toPersistedSykmeldingDiagnoseInfo(),
            aktivitet =
                payload.values.aktivitet.fromOpprettSykmeldingToPersistedSykmeldingAktivitetList(),
            pasient = pasient.toPersistedSykmeldingPasient(),
            sykmelder = sykmelder.toPersistedSykmeldingSykmelder(payload.meta.sykmelderHpr),
            bidiagnoser =
                payload.values.bidiagnoser
                    .fromOpprettSykmeldingToPersistedSykmeldingDiagnoseInfoList(),
            svangerskapsrelatert = payload.values.svangerskapsrelatert,
            pasientenSkalSkjermes = payload.values.pasientenSkalSkjermes,
            meldinger = payload.values.meldinger.toPersistedSykmeldingMeldinger(),
            yrkesskade = payload.values.yrkesskade.toPersistedSykmeldingYrkesskade(),
            arbeidsgiver = payload.values.arbeidsgiver.toPersistedSykmeldingArbeidsgiver(),
            tilbakedatering = payload.values.tilbakedatering.toPersistedSykmeldingTilbakedatering(),
            regelResultat = ruleResult.toPersistedSykmeldingRuleResult(),
        )
    }

    fun mapSykmeldingRecordToPersistedSykmelding(
        sykmeldingRecord: no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord,
        person: Person,
        sykmelder: Sykmelder,
    ): PersistedSykmelding {
        val sykmeldingRecordMedisinskVurdering = sykmeldingRecord.sykmelding.medisinskVurdering

        return PersistedSykmelding(
            hoveddiagnose =
                sykmeldingRecordMedisinskVurdering.hovedDiagnose
                    .toPersistedSykmeldingDiagnoseInfo(),
            aktivitet = sykmeldingRecord.sykmelding.aktivitet.toPersistedSykmeldingAktivitetList(),
            sykmeldingId = sykmeldingRecord.sykmelding.id,
            pasient = sykmeldingRecord.sykmelding.pasient.toPersistedSykmeldingPasient(person),
            sykmelder = mapSykmeldingRecordToPersistedSykmeldingSykmelder(sykmelder),
            bidiagnoser =
                sykmeldingRecordMedisinskVurdering.biDiagnoser
                    .toPersistedSykmeldingDiagnoseInfoList(),
            svangerskapsrelatert = sykmeldingRecordMedisinskVurdering.svangerskap,
            pasientenSkalSkjermes = sykmeldingRecordMedisinskVurdering.skjermetForPasient,
            meldinger = mapSykmeldingRecordToPersistedSykmeldingMeldinger(sykmeldingRecord),
            yrkesskade = mapSykmeldingRecordToPersistedSykmeldingYrkesskade(sykmeldingRecord),
            arbeidsgiver = mapSykmeldingRecordToPersistedSykmeldingArbeidsgiver(sykmeldingRecord),
            tilbakedatering =
                mapSykmeldingRecordToPersistedSykmeldingTilbakedatering(
                    sykmeldingRecord,
                ),
            regelResultat = sykmeldingRecord.validation.toPersistedSykmeldingResult(),
        )
    }

    fun mapLegekontorOrgnr(
        sykmeldingRecord: no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
    ): String? {
        return when (val metadata = sykmeldingRecord.metadata) {
            is no.nav.tsm.sykmelding.input.core.model.metadata.Digital -> metadata.orgnummer
            is no.nav.tsm.sykmelding.input.core.model.metadata.Papir ->
                metadata.sender.ids.firstOrNull().let { it?.id }
            is no.nav.tsm.sykmelding.input.core.model.metadata.EmottakEnkel ->
                metadata.sender.ids.firstOrNull().let { it?.id }
                    ?: error("No orgnr found in sender object (EmottakEnkel)")
            is no.nav.tsm.sykmelding.input.core.model.metadata.EDIEmottak ->
                metadata.sender.ids.firstOrNull().let { it?.id }
                    ?: error("No orgnr found in sender object (EDIEmottak)")
            is no.nav.tsm.sykmelding.input.core.model.metadata.Utenlandsk -> null
            is no.nav.tsm.sykmelding.input.core.model.metadata.Egenmeldt -> null
        }
    }

    fun mapLegekontorTlf(
        sykmeldingRecord: no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
    ): String? {
        return when (val sykmelding = sykmeldingRecord.sykmelding) {
            is no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding -> {
                sykmelding.behandler.kontaktinfo
                    .firstOrNull() {
                        it.type ==
                            no.nav.tsm.sykmelding.input.core.model.metadata.KontaktinfoType.TLF
                    }
                    ?.value
            }
            is no.nav.tsm.sykmelding.input.core.model.Papirsykmelding -> {
                sykmelding.behandler.kontaktinfo
                    .firstOrNull() {
                        it.type ==
                            no.nav.tsm.sykmelding.input.core.model.metadata.KontaktinfoType.TLF
                    }
                    ?.value
            }
            is no.nav.tsm.sykmelding.input.core.model.XmlSykmelding -> {
                sykmelding.behandler.kontaktinfo
                    .firstOrNull() {
                        it.type ==
                            no.nav.tsm.sykmelding.input.core.model.metadata.KontaktinfoType.TLF
                    }
                    ?.value
            }
            else -> null
        }
    }

    fun mapHprNummer(value: no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord): String {
        return when (val sykmelding = value.sykmelding) {
            is no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding -> {
                sykmelding.sykmelder.ids
                    .find {
                        it.type == no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType.HPR
                    }
                    ?.id
                    ?: error(
                        "No HPR number found in Sykmelder-object. sykmelder id type \n ${sykmelding.sykmelder.ids.first().type.name}"
                    )
            }
            is no.nav.tsm.sykmelding.input.core.model.Papirsykmelding -> {
                sykmelding.sykmelder.ids
                    .find {
                        it.type == no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType.HPR
                    }
                    ?.id
                    ?: error(
                        "No HPR number found in Sykmelder-object. sykmelder id type \n ${sykmelding.sykmelder.ids.first().type.name}"
                    )
            }
            is no.nav.tsm.sykmelding.input.core.model.XmlSykmelding -> {
                sykmelding.sykmelder.ids
                    .find {
                        it.type == no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType.HPR
                    }
                    ?.id
                //                    ?: error("No HPR number found in Sykmelder-object")
                ?: error(
                        "No HPR number found in Sykmelder-object. sykmelder id type \n ${sykmelding.sykmelder.ids.first().type.name}"
                    )
            }
            else -> {
                logger.warn("Sykmelding type is not SykInnSykmelding, cannot map HPR number")
                return "No sykmelder exists as this must be a utenlandsk sykmelding"
            }
        }
    }

    private fun OpprettSykmeldingDiagnoseInfo.toPersistedSykmeldingDiagnoseInfo():
        PersistedSykmeldingDiagnoseInfo {
        return PersistedSykmeldingDiagnoseInfo(
            system = system,
            code = code,
            text = DiagnosekodeMapper.findTextFromDiagnoseSystem(system, code)
                    ?: "Unknown diagnosis code: $code",
        )
    }

    private fun no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem.toDiagnoseSystem():
        DiagnoseSystem {
        return when (this) {
            no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem.ICD10 -> DiagnoseSystem.ICD10
            no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem.ICPC2 -> DiagnoseSystem.ICPC2
            else -> {
                logger.error("Unknown DiagnoseSystem: $this")
                throw IllegalArgumentException("Unknown DiagnoseSystem: $this")
            }
        }
    }

    private fun Person.toPersistedSykmeldingPasient(): PersistedSykmeldingPasient {
        return PersistedSykmeldingPasient(
            navn = navn,
            ident = ident,
            fodselsdato = fodselsdato,
        )
    }

    private fun Sykmelder.toPersistedSykmeldingSykmelder(
        hprNummer: String
    ): PersistedSykmeldingSykmelder {
        return PersistedSykmeldingSykmelder(
            godkjenninger = godkjenninger.toPersistedSykmeldingHprGodkjenning(),
            ident = this.ident,
            hprNummer = hprNummer,
            fornavn = this.navn?.fornavn,
            mellomnavn = this.navn?.mellomnavn,
            etternavn = this.navn?.etternavn,
        )
    }

    private fun OpprettSykmeldingMeldinger.toPersistedSykmeldingMeldinger():
        PersistedSykmeldingMeldinger {
        return PersistedSykmeldingMeldinger(
            tilNav = tilNav,
            tilArbeidsgiver = tilArbeidsgiver,
        )
    }

    private fun OpprettSykmeldingTilbakedatering?.toPersistedSykmeldingTilbakedatering():
        PersistedSykmeldingTilbakedatering? {
        if (this == null) return null

        return PersistedSykmeldingTilbakedatering(
            startdato = startdato,
            begrunnelse = begrunnelse,
        )
    }

    private fun OpprettSykmeldingYrkesskade?.toPersistedSykmeldingYrkesskade():
        PersistedSykmeldingYrkesskade? {
        if (this == null) return null

        return PersistedSykmeldingYrkesskade(
            yrkesskade = yrkesskade,
            skadedato = skadedato,
        )
    }

    private fun OpprettSykmeldingArbeidsgiver?.toPersistedSykmeldingArbeidsgiver():
        PersistedSykmeldingArbeidsgiver? {
        if (this == null) return null

        return PersistedSykmeldingArbeidsgiver(
            harFlere = harFlere,
            arbeidsgivernavn = arbeidsgivernavn,
        )
    }

    private fun List<OpprettSykmeldingDiagnoseInfo>
        .fromOpprettSykmeldingToPersistedSykmeldingDiagnoseInfoList():
        List<PersistedSykmeldingDiagnoseInfo> {
        if (this.isEmpty()) return emptyList()
        val diagnoseInfo = mutableListOf<PersistedSykmeldingDiagnoseInfo>()

        this.forEach { info ->
            diagnoseInfo.add(
                PersistedSykmeldingDiagnoseInfo(
                    system = info.system,
                    code = info.code,
                    text = DiagnosekodeMapper.findTextFromDiagnoseSystem(info.system, info.code)
                            ?: "Unknown diagnosis code: $info.code",
                ),
            )
        }
        return diagnoseInfo
    }

    private fun List<OpprettSykmeldingAktivitet>
        .fromOpprettSykmeldingToPersistedSykmeldingAktivitetList():
        List<PersistedSykmeldingAktivitet> {
        if (this.isEmpty()) return emptyList()

        val aktivitet = mutableListOf<PersistedSykmeldingAktivitet>()
        this.forEach { opprettSykmeldingAktivitet ->
            when (opprettSykmeldingAktivitet) {
                is OpprettSykmeldingAktivitet.IkkeMulig ->
                    aktivitet.add(
                        PersistedSykmeldingAktivitet.IkkeMulig(
                            fom = opprettSykmeldingAktivitet.fom,
                            tom = opprettSykmeldingAktivitet.tom,
                        ),
                    )
                is OpprettSykmeldingAktivitet.Gradert ->
                    aktivitet.add(
                        PersistedSykmeldingAktivitet.Gradert(
                            grad = opprettSykmeldingAktivitet.grad,
                            fom = opprettSykmeldingAktivitet.fom,
                            tom = opprettSykmeldingAktivitet.tom,
                            reisetilskudd = opprettSykmeldingAktivitet.reisetilskudd,
                        ),
                    )
                is OpprettSykmeldingAktivitet.Avventende ->
                    aktivitet.add(
                        PersistedSykmeldingAktivitet.Avventende(
                            innspillTilArbeidsgiver =
                                opprettSykmeldingAktivitet.innspillTilArbeidsgiver,
                            fom = opprettSykmeldingAktivitet.fom,
                            tom = opprettSykmeldingAktivitet.tom,
                        ),
                    )
                is OpprettSykmeldingAktivitet.Behandlingsdager ->
                    aktivitet.add(
                        PersistedSykmeldingAktivitet.Behandlingsdager(
                            antallBehandlingsdager =
                                opprettSykmeldingAktivitet.antallBehandlingsdager,
                            fom = opprettSykmeldingAktivitet.fom,
                            tom = opprettSykmeldingAktivitet.tom,
                        ),
                    )
                is OpprettSykmeldingAktivitet.Reisetilskudd ->
                    aktivitet.add(
                        PersistedSykmeldingAktivitet.Reisetilskudd(
                            fom = opprettSykmeldingAktivitet.fom,
                            tom = opprettSykmeldingAktivitet.tom,
                        ),
                    )
            }
        }

        return aktivitet
    }

    private fun List<HprGodkjenning>.toPersistedSykmeldingHprGodkjenning():
        List<PersistedSykmeldingHprGodkjenning> {
        if (this.isEmpty()) return emptyList()

        val hprGodkjenninger = mutableListOf<PersistedSykmeldingHprGodkjenning>()

        this.forEach { godkjenning ->
            hprGodkjenninger.add(
                PersistedSykmeldingHprGodkjenning(
                    helsepersonellkategori =
                        godkjenning.helsepersonellkategori.toPersistedSykmeldingHprKode(),
                    autorisasjon = godkjenning.autorisasjon.toPersistedSykmeldingHprKode(),
                    tillegskompetanse =
                        godkjenning.tillegskompetanse.toPersistedSykmeldingTilleggskompetanse(),
                ),
            )
        }

        return hprGodkjenninger
    }

    private fun RegulaResult.toPersistedSykmeldingRuleResult(): PersistedSykmeldingRuleResult {
        return when (this) {
            is RegulaResult.Ok ->
                PersistedSykmeldingRuleResult(
                    result = this.status.name,
                    meldingTilSender = null,
                )
            is RegulaResult.NotOk ->
                PersistedSykmeldingRuleResult(
                    result = this.status.name,
                    meldingTilSender = outcome.reason.sykmelder,
                )
        }
    }

    private fun List<HprTilleggskompetanse>?.toPersistedSykmeldingTilleggskompetanse():
        List<PersistedSykmeldingHprTilleggskompetanse>? {
        if (this == null) return null
        if (this.isEmpty()) return emptyList()

        val tillegskompetanse = mutableListOf<PersistedSykmeldingHprTilleggskompetanse>()
        this.forEach { kompetanse ->
            tillegskompetanse.add(
                PersistedSykmeldingHprTilleggskompetanse(
                    avsluttetStatus = kompetanse.avsluttetStatus.toPersistedSykmeldingHprKode(),
                    eTag = kompetanse.eTag,
                    gyldig = kompetanse.gyldig.toPersistedSykmeldingHprGyldighetsPeriode(),
                    id = kompetanse.id,
                    type = kompetanse.type.toPersistedSykmeldingHprKode(),
                ),
            )
        }

        return tillegskompetanse
    }

    private fun HprKode?.toPersistedSykmeldingHprKode(): PersistedSykmeldingHprKode? {
        if (this == null) return null

        return PersistedSykmeldingHprKode(
            aktiv = aktiv,
            oid = oid,
            verdi = verdi,
        )
    }

    private fun HprGyldighetsPeriode?.toPersistedSykmeldingHprGyldighetsPeriode():
        PersistedSykmeldingHprGyldighetsPeriode? {
        if (this == null) return null
        return PersistedSykmeldingHprGyldighetsPeriode(
            fra = fra,
            til = til,
        )
    }

    private fun List<Aktivitet>.toPersistedSykmeldingAktivitetList():
        List<PersistedSykmeldingAktivitet> {
        if (this.isEmpty()) return emptyList()

        val aktiviteter = mutableListOf<PersistedSykmeldingAktivitet>()
        this.forEach { sykmeldingRecordAktivitet ->
            when (sykmeldingRecordAktivitet) {
                is AktivitetIkkeMulig ->
                    PersistedSykmeldingAktivitet.IkkeMulig(
                        fom = sykmeldingRecordAktivitet.fom.toString(),
                        tom = sykmeldingRecordAktivitet.tom.toString(),
                    )
                is Gradert ->
                    PersistedSykmeldingAktivitet.Gradert(
                        grad = sykmeldingRecordAktivitet.grad,
                        fom = sykmeldingRecordAktivitet.fom.toString(),
                        tom = sykmeldingRecordAktivitet.tom.toString(),
                        reisetilskudd = sykmeldingRecordAktivitet.reisetilskudd,
                    )
                is Avventende ->
                    PersistedSykmeldingAktivitet.Avventende(
                        innspillTilArbeidsgiver = sykmeldingRecordAktivitet.innspillTilArbeidsgiver,
                        fom = sykmeldingRecordAktivitet.fom.toString(),
                        tom = sykmeldingRecordAktivitet.tom.toString(),
                    )
                is Behandlingsdager ->
                    PersistedSykmeldingAktivitet.Behandlingsdager(
                        antallBehandlingsdager = sykmeldingRecordAktivitet.antallBehandlingsdager,
                        fom = sykmeldingRecordAktivitet.fom.toString(),
                        tom = sykmeldingRecordAktivitet.tom.toString(),
                    )
                is Reisetilskudd ->
                    PersistedSykmeldingAktivitet.Reisetilskudd(
                        fom = sykmeldingRecordAktivitet.fom.toString(),
                        tom = sykmeldingRecordAktivitet.tom.toString(),
                    )
            }
        }
        return aktiviteter
    }

    private fun DiagnoseInfo?.toPersistedSykmeldingDiagnoseInfo():
        PersistedSykmeldingDiagnoseInfo? {
        if (this == null) return null

        return PersistedSykmeldingDiagnoseInfo(
            system = system.toDiagnoseSystem(),
            code = kode,
            text = DiagnosekodeMapper.findTextFromDiagnoseSystem(system.toDiagnoseSystem(), kode)
                    ?: "Unknown diagnosis code: $kode",
        )
    }

    private fun no.nav.tsm.sykmelding.input.core.model.ValidationResult
        .toPersistedSykmeldingResult(): PersistedSykmeldingRuleResult {
        if (this.status === no.nav.tsm.sykmelding.input.core.model.RuleType.OK) {
            return PersistedSykmeldingRuleResult(
                result = RuleType.OK.name,
                meldingTilSender = null,
            )
        }

        return when (val rule = rules.first()) {
            is no.nav.tsm.sykmelding.input.core.model.InvalidRule -> {
                PersistedSykmeldingRuleResult(
                    result = status.name,
                    meldingTilSender = rule.reason.sykmelder,
                )
            }
            is no.nav.tsm.sykmelding.input.core.model.PendingRule -> {
                PersistedSykmeldingRuleResult(
                    result = status.name,
                    meldingTilSender = rule.reason.sykmelder,
                )
            }
            is no.nav.tsm.sykmelding.input.core.model.OKRule -> {
                PersistedSykmeldingRuleResult(
                    result = status.name,
                    meldingTilSender = null,
                )
            }
        }
    }

    private fun mapSykmeldingRecordToPersistedSykmeldingArbeidsgiver(
        sykmeldingRecord: no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
    ): PersistedSykmeldingArbeidsgiver? {
        return when (val value = sykmeldingRecord.sykmelding) {
            is no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding -> {
                val arbeidsgiver = value.arbeidsgiver ?: return null

                getArbeidsgiverInfo(arbeidsgiver)
            }
            is no.nav.tsm.sykmelding.input.core.model.Papirsykmelding -> {
                getArbeidsgiverInfo(value.arbeidsgiver)
            }
            is no.nav.tsm.sykmelding.input.core.model.XmlSykmelding -> {
                getArbeidsgiverInfo(value.arbeidsgiver)
            }
            else -> {
                return null
            }
        }
    }

    private fun getArbeidsgiverInfo(
        arbeidsgiver: ArbeidsgiverInfo
    ): PersistedSykmeldingArbeidsgiver? {
        return when (arbeidsgiver) {
            is no.nav.tsm.sykmelding.input.core.model.EnArbeidsgiver -> {
                null
            }
            is no.nav.tsm.sykmelding.input.core.model.FlereArbeidsgivere -> {
                PersistedSykmeldingArbeidsgiver(
                    harFlere = true,
                    arbeidsgivernavn = arbeidsgiver.navn ?: "Manglende arbeidsgivernavn",
                )
            }
            is no.nav.tsm.sykmelding.input.core.model.IngenArbeidsgiver -> {
                null
            }
        }
    }

    private fun Pasient.toPersistedSykmeldingPasient(pasient: Person): PersistedSykmeldingPasient {
        return PersistedSykmeldingPasient(
            navn = pasient.navn,
            ident = fnr,
            fodselsdato = pasient.fodselsdato,
        )
    }

    private fun List<DiagnoseInfo>?.toPersistedSykmeldingDiagnoseInfoList():
        List<PersistedSykmeldingDiagnoseInfo> {
        if (this == null) return emptyList()
        if (this.isEmpty()) return emptyList()
        val diagnoseInfo = mutableListOf<PersistedSykmeldingDiagnoseInfo>()

        this.forEach { info ->
            diagnoseInfo.add(
                PersistedSykmeldingDiagnoseInfo(
                    system = info.system.toDiagnoseSystem(),
                    code = info.kode,
                    text =
                        DiagnosekodeMapper.findTextFromDiagnoseSystem(
                            info.system.toDiagnoseSystem(),
                            info.kode,
                        )
                            ?: "Unknown diagnosis code: ${info.kode}",
                ),
            )
        }

        return diagnoseInfo
    }

    private fun mapSykmeldingRecordToPersistedSykmeldingSykmelder(
        sykmelder: Sykmelder
    ): PersistedSykmeldingSykmelder {
        return PersistedSykmeldingSykmelder(
            godkjenninger = sykmelder.godkjenninger.toPersistedSykmeldingHprGodkjenning(),
            ident = sykmelder.ident,
            hprNummer = sykmelder.hpr,
            fornavn = sykmelder.navn?.fornavn,
            mellomnavn = sykmelder.navn?.mellomnavn,
            etternavn = sykmelder.navn?.etternavn,
        )
    }

    private fun mapSykmeldingRecordToPersistedSykmeldingYrkesskade(
        sykmeldingRecord: no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
    ): PersistedSykmeldingYrkesskade? {
        val sykmeldingRecordMedisinskVurdering = sykmeldingRecord.sykmelding.medisinskVurdering
        return when (sykmeldingRecord.sykmelding) {
            is no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding -> {
                createPersistedSykmeldingYrkesskade(sykmeldingRecordMedisinskVurdering)
            }
            is no.nav.tsm.sykmelding.input.core.model.Papirsykmelding -> {
                createPersistedSykmeldingYrkesskade(sykmeldingRecordMedisinskVurdering)
            }
            is no.nav.tsm.sykmelding.input.core.model.XmlSykmelding -> {
                createPersistedSykmeldingYrkesskade(sykmeldingRecordMedisinskVurdering)
            }
            else -> {
                return null
            }
        }
    }

    private fun createPersistedSykmeldingYrkesskade(
        vurdering: MedisinskVurdering
    ): PersistedSykmeldingYrkesskade? {
        if (vurdering.yrkesskade != null) {
            return PersistedSykmeldingYrkesskade(
                yrkesskade = true,
                skadedato = vurdering.yrkesskade?.yrkesskadeDato,
            )
        }
        return null
    }

    private fun mapSykmeldingRecordToPersistedSykmeldingTilbakedatering(
        sykmeldingRecord: no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
    ): PersistedSykmeldingTilbakedatering? {
        return when (val value = sykmeldingRecord.sykmelding) {
            is no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding -> {
                createPersistedSykmeldingTilbakedatering(value.tilbakedatering)
            }
            is no.nav.tsm.sykmelding.input.core.model.Papirsykmelding -> {
                createPersistedSykmeldingTilbakedatering(value.tilbakedatering)
            }
            is no.nav.tsm.sykmelding.input.core.model.XmlSykmelding -> {
                createPersistedSykmeldingTilbakedatering(value.tilbakedatering)
            }
            else -> {
                return null
            }
        }
    }

    private fun createPersistedSykmeldingTilbakedatering(
        tilbakedatering: Tilbakedatering?
    ): PersistedSykmeldingTilbakedatering? {
        return tilbakedatering?.let {
            val kontaktDato = it.kontaktDato
            val begrunnelse = it.begrunnelse
            if (kontaktDato != null && begrunnelse != null) {
                PersistedSykmeldingTilbakedatering(
                    startdato = kontaktDato,
                    begrunnelse = begrunnelse,
                )
            } else {
                null
            }
        }
    }

    private fun mapToPersistedSykmeldingMeldinger(
        arbeidsgiver: ArbeidsgiverInfo?,
        bistandNav: String?
    ): PersistedSykmeldingMeldinger {
        if (arbeidsgiver is no.nav.tsm.sykmelding.input.core.model.FlereArbeidsgivere) {
            return PersistedSykmeldingMeldinger(
                tilNav = bistandNav,
                tilArbeidsgiver = arbeidsgiver.meldingTilArbeidsgiver,
            )
        }
        return PersistedSykmeldingMeldinger(
            tilNav = bistandNav,
            tilArbeidsgiver = null,
        )
    }

    private fun mapSykmeldingRecordToPersistedSykmeldingMeldinger(
        sykmeldingRecord: no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
    ): PersistedSykmeldingMeldinger {
        return when (val value = sykmeldingRecord.sykmelding) {
            is no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding -> {
                val arbeidsgiver = value.arbeidsgiver
                val bistandNav = value.bistandNav?.beskrivBistand
                mapToPersistedSykmeldingMeldinger(arbeidsgiver, bistandNav)
            }
            is no.nav.tsm.sykmelding.input.core.model.Papirsykmelding -> {
                val arbeidsgiver = value.arbeidsgiver
                val bistandNav = value.bistandNav?.beskrivBistand
                mapToPersistedSykmeldingMeldinger(arbeidsgiver, bistandNav)
            }
            is no.nav.tsm.sykmelding.input.core.model.XmlSykmelding -> {
                val arbeidsgiver = value.arbeidsgiver
                val bistandNav = value.bistandNav?.beskrivBistand
                mapToPersistedSykmeldingMeldinger(arbeidsgiver, bistandNav)
            }
            else -> {
                // How should we handle this for other cases such as Utenlandsk? Empty objects?
                PersistedSykmeldingMeldinger(
                    tilNav = null,
                    tilArbeidsgiver = null,
                )
            }
        }
    }

    fun no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord.isBeforeYear(year: Int): Boolean {
        val tom = this.sykmelding.aktivitet.first().tom
        return tom.isBefore(
            LocalDate.of(
                year,
                Month.JANUARY,
                1,
            ),
        )
    }
}
