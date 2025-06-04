package no.nav.tsm.syk_inn_api.sykmelding.persistence

import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.common.DiagnosekodeMapper
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprGodkjenning
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprGyldighetsPeriode
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprKode
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprSykmelder
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprTilleggskompetanse
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingArbeidsgiver
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingDiagnoseInfo
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingMeldinger
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingTilbakedatering
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingYrkesskade
import no.nav.tsm.syk_inn_api.sykmelding.SykmeldingPayload
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.Digital
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.EDIEmottak
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.EmottakEnkel
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.KontaktinfoType
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.Papir
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.PersonIdType
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.DigitalSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.EnArbeidsgiver
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.FlereArbeidsgivere
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.IngenArbeidsgiver
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.KafkaDiagnoseInfo
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.KafkaDiagnoseSystem
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.Papirsykmelding
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecord
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecordAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecordArbeidsgiverInfo
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecordMedisinskVurdering
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecordPasient
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecordTilbakedatering
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.XmlSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.rules.InvalidRule
import no.nav.tsm.syk_inn_api.sykmelding.rules.OKRule
import no.nav.tsm.syk_inn_api.sykmelding.rules.PendingRule
import no.nav.tsm.syk_inn_api.sykmelding.rules.ValidationResult
import org.slf4j.LoggerFactory

object PersistedSykmeldingMapper {

    private val logger = LoggerFactory.getLogger(PersistedSykmeldingMapper::class.java)

    fun mapSykmeldingPayloadToPersistedSykmelding(
        payload: SykmeldingPayload,
        sykmeldingId: String,
        pasient: Person,
        sykmelder: HprSykmelder,
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
        sykmeldingRecord: SykmeldingRecord,
        person: Person,
        sykmelder: HprSykmelder,
    ): PersistedSykmelding {
        val sykmeldingRecordMedisinskVurdering =
            sykmeldingRecord.sykmelding.sykmeldingRecordMedisinskVurdering

        return PersistedSykmelding(
            hoveddiagnose =
                sykmeldingRecordMedisinskVurdering.hovedDiagnose
                    .toPersistedSykmeldingDiagnoseInfo(),
            aktivitet =
                sykmeldingRecord.sykmelding.sykmeldingRecordAktivitet
                    .toPersistedSykmeldingAktivitetList(),
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
                mapSykmeldingRecordToPersistedSykmeldingTilbakedatering(sykmeldingRecord),
            regelResultat = sykmeldingRecord.validation.toPersistedSykmeldingResult(),
        )
    }

    fun mapLegekontorOrgnr(sykmeldingRecord: SykmeldingRecord): String {
        return when (val metadata = sykmeldingRecord.metadata) {
            is Digital -> metadata.orgnummer
            is Papir -> metadata.sender.ids.firstOrNull().let { it?.id }
                    ?: error("No orgnr found in sender object")
            is EmottakEnkel -> metadata.sender.ids.firstOrNull().let { it?.id }
                    ?: error("No orgnr found in sender object")
            is EDIEmottak -> metadata.sender.ids.firstOrNull().let { it?.id }
                    ?: error("No orgnr found in sender object")
            else -> "Missing legekontor orgnr"
        }
    }

    fun mapLegekontorTlf(sykmeldingRecord: SykmeldingRecord): String? {
        return when (val sykmelding = sykmeldingRecord.sykmelding) {
            is DigitalSykmelding -> {
                sykmelding.behandler.kontaktinfo
                    .firstOrNull() { it.type == KontaktinfoType.TLF }
                    ?.value
            }
            is Papirsykmelding -> {
                sykmelding.behandler.kontaktinfo
                    .firstOrNull() { it.type == KontaktinfoType.TLF }
                    ?.value
            }
            is XmlSykmelding -> {
                sykmelding.behandler.kontaktinfo
                    .firstOrNull() { it.type == KontaktinfoType.TLF }
                    ?.value
            }
            else -> null
        }
    }

    fun mapHprNummer(value: SykmeldingRecord): String {
        return when (val sykmelding = value.sykmelding) {
            is DigitalSykmelding -> {
                sykmelding.sykmelder.ids.firstOrNull { it.type == PersonIdType.HPR }?.id
                    ?: error("No HPR number found in Sykmelder-object")
            }
            is Papirsykmelding -> {
                sykmelding.sykmelder.ids.firstOrNull { it.type == PersonIdType.HPR }?.id
                    ?: error("No HPR number found in Sykmelder-object")
            }
            is XmlSykmelding -> {
                sykmelding.sykmelder.ids.firstOrNull { it.type == PersonIdType.HPR }?.id
                    ?: error("No HPR number found in Sykmelder-object")
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

    private fun KafkaDiagnoseSystem.toDiagnoseSystem(): DiagnoseSystem {
        return when (this) {
            KafkaDiagnoseSystem.ICD10 -> DiagnoseSystem.ICD10
            KafkaDiagnoseSystem.ICPC2 -> DiagnoseSystem.ICPC2
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

    private fun HprSykmelder.toPersistedSykmeldingSykmelder(
        hprNummer: String
    ): PersistedSykmeldingSykmelder {
        return PersistedSykmeldingSykmelder(
            godkjenninger = godkjenninger.toPersistedSykmeldingHprGodkjenning(),
            ident = this.fnr,
            hprNummer = hprNummer,
            fornavn = this.fornavn,
            mellomnavn = this.mellomnavn,
            etternavn = this.etternavn,
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

    private fun List<SykmeldingRecordAktivitet>.toPersistedSykmeldingAktivitetList():
        List<PersistedSykmeldingAktivitet> {
        if (this.isEmpty()) return emptyList()

        val aktiviteter = mutableListOf<PersistedSykmeldingAktivitet>()
        this.forEach { sykmeldingRecordAktivitet ->
            when (sykmeldingRecordAktivitet) {
                is SykmeldingRecordAktivitet.AktivitetIkkeMulig ->
                    PersistedSykmeldingAktivitet.IkkeMulig(
                        fom = sykmeldingRecordAktivitet.fom.toString(),
                        tom = sykmeldingRecordAktivitet.tom.toString(),
                    )
                is SykmeldingRecordAktivitet.Gradert ->
                    PersistedSykmeldingAktivitet.Gradert(
                        grad = sykmeldingRecordAktivitet.grad,
                        fom = sykmeldingRecordAktivitet.fom.toString(),
                        tom = sykmeldingRecordAktivitet.tom.toString(),
                        reisetilskudd = sykmeldingRecordAktivitet.reisetilskudd,
                    )
                is SykmeldingRecordAktivitet.Avventende ->
                    PersistedSykmeldingAktivitet.Avventende(
                        innspillTilArbeidsgiver = sykmeldingRecordAktivitet.innspillTilArbeidsgiver,
                        fom = sykmeldingRecordAktivitet.fom.toString(),
                        tom = sykmeldingRecordAktivitet.tom.toString(),
                    )
                is SykmeldingRecordAktivitet.Behandlingsdager ->
                    PersistedSykmeldingAktivitet.Behandlingsdager(
                        antallBehandlingsdager = sykmeldingRecordAktivitet.antallBehandlingsdager,
                        fom = sykmeldingRecordAktivitet.fom.toString(),
                        tom = sykmeldingRecordAktivitet.tom.toString(),
                    )
                is SykmeldingRecordAktivitet.Reisetilskudd ->
                    PersistedSykmeldingAktivitet.Reisetilskudd(
                        fom = sykmeldingRecordAktivitet.fom.toString(),
                        tom = sykmeldingRecordAktivitet.tom.toString(),
                    )
            }
        }
        return aktiviteter
    }

    private fun KafkaDiagnoseInfo?.toPersistedSykmeldingDiagnoseInfo():
        PersistedSykmeldingDiagnoseInfo? {
        if (this == null) return null

        return PersistedSykmeldingDiagnoseInfo(
            system = system.toDiagnoseSystem(),
            code = kode,
            text = DiagnosekodeMapper.findTextFromDiagnoseSystem(system.toDiagnoseSystem(), kode)
                    ?: "Unknown diagnosis code: $kode",
        )
    }

    private fun ValidationResult.toPersistedSykmeldingResult(): PersistedSykmeldingRuleResult {
        return when (val rule = rules.first()) {
            is InvalidRule -> {
                PersistedSykmeldingRuleResult(
                    result = status.name,
                    meldingTilSender = rule.reason.sykmelder,
                )
            }
            is OKRule -> {
                PersistedSykmeldingRuleResult(
                    result = status.name,
                    meldingTilSender = null,
                )
            }
            is PendingRule -> {
                PersistedSykmeldingRuleResult(
                    result = status.name,
                    meldingTilSender = rule.reason.sykmelder,
                )
            }
        }
    }

    private fun mapSykmeldingRecordToPersistedSykmeldingArbeidsgiver(
        sykmeldingRecord: SykmeldingRecord
    ): PersistedSykmeldingArbeidsgiver? {
        return when (val value = sykmeldingRecord.sykmelding) {
            is DigitalSykmelding -> {
                getArbeidsgiverInfo(value.arbeidsgiver)
            }
            is Papirsykmelding -> {
                getArbeidsgiverInfo(value.arbeidsgiver)
            }
            is XmlSykmelding -> {
                getArbeidsgiverInfo(value.arbeidsgiver)
            }
            else -> {
                return null
            }
        }
    }

    private fun getArbeidsgiverInfo(
        arbeidsgiver: SykmeldingRecordArbeidsgiverInfo
    ): PersistedSykmeldingArbeidsgiver? {
        return when (arbeidsgiver) {
            is EnArbeidsgiver -> {
                null
            }
            is FlereArbeidsgivere -> {
                PersistedSykmeldingArbeidsgiver(
                    harFlere = true,
                    arbeidsgivernavn = arbeidsgiver.navn ?: "Manglende arbeidsgivernavn",
                )
            }
            is IngenArbeidsgiver -> {
                null
            }
        }
    }

    private fun SykmeldingRecordPasient.toPersistedSykmeldingPasient(
        pasient: Person
    ): PersistedSykmeldingPasient {
        return PersistedSykmeldingPasient(
            navn = pasient.navn,
            ident = fnr,
            fodselsdato = pasient.fodselsdato,
        )
    }

    private fun List<KafkaDiagnoseInfo>?.toPersistedSykmeldingDiagnoseInfoList():
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
        sykmelder: HprSykmelder
    ): PersistedSykmeldingSykmelder {
        requireNotNull(sykmelder.hprNummer) { "Sykmelder HPR number is required" }
        return PersistedSykmeldingSykmelder(
            godkjenninger = sykmelder.godkjenninger.toPersistedSykmeldingHprGodkjenning(),
            ident = sykmelder.fnr,
            hprNummer = sykmelder.hprNummer,
            fornavn = sykmelder.fornavn,
            mellomnavn = sykmelder.mellomnavn,
            etternavn = sykmelder.etternavn,
        )
    }

    private fun createPersistedSykmeldingSykmelder(
        sykmelder: HprSykmelder
    ): PersistedSykmeldingSykmelder {
        requireNotNull(sykmelder.hprNummer) { "Sykmelder HPR number is required" }
        return PersistedSykmeldingSykmelder(
            godkjenninger = sykmelder.godkjenninger.toPersistedSykmeldingHprGodkjenning(),
            ident = sykmelder.fnr,
            hprNummer = sykmelder.hprNummer,
            fornavn = sykmelder.fornavn,
            mellomnavn = sykmelder.mellomnavn,
            etternavn = sykmelder.etternavn,
        )
    }

    private fun mapSykmeldingRecordToPersistedSykmeldingYrkesskade(
        sykmeldingRecord: SykmeldingRecord
    ): PersistedSykmeldingYrkesskade? {
        val sykmeldingRecordMedisinskVurdering =
            sykmeldingRecord.sykmelding.sykmeldingRecordMedisinskVurdering
        return when (sykmeldingRecord.sykmelding) {
            is DigitalSykmelding -> {
                createPersistedSykmeldingYrkesskade(sykmeldingRecordMedisinskVurdering)
            }
            is Papirsykmelding -> {
                createPersistedSykmeldingYrkesskade(sykmeldingRecordMedisinskVurdering)
            }
            is XmlSykmelding -> {
                createPersistedSykmeldingYrkesskade(sykmeldingRecordMedisinskVurdering)
            }
            else -> {
                return null
            }
        }
    }

    private fun createPersistedSykmeldingYrkesskade(
        vurdering: SykmeldingRecordMedisinskVurdering
    ): PersistedSykmeldingYrkesskade? {
        if (vurdering.yrkesskade != null) {
            return PersistedSykmeldingYrkesskade(
                yrkesskade = true,
                skadedato = vurdering.yrkesskade.yrkesskadeDato,
            )
        }
        return null
    }

    private fun mapSykmeldingRecordToPersistedSykmeldingTilbakedatering(
        sykmeldingRecord: SykmeldingRecord
    ): PersistedSykmeldingTilbakedatering? {
        return when (val value = sykmeldingRecord.sykmelding) {
            is DigitalSykmelding -> {
                createPersistedSykmeldingTilbakedatering(value.tilbakedatering)
            }
            is Papirsykmelding -> {
                createPersistedSykmeldingTilbakedatering(value.tilbakedatering)
            }
            is XmlSykmelding -> {
                createPersistedSykmeldingTilbakedatering(value.tilbakedatering)
            }
            else -> {
                return null
            }
        }
    }

    private fun createPersistedSykmeldingTilbakedatering(
        tilbakedatering: SykmeldingRecordTilbakedatering?
    ): PersistedSykmeldingTilbakedatering? {
        if (tilbakedatering == null) return null
        requireNotNull(tilbakedatering.kontaktDato)
        requireNotNull(tilbakedatering.begrunnelse)
        return PersistedSykmeldingTilbakedatering(
            startdato = tilbakedatering.kontaktDato,
            begrunnelse = tilbakedatering.begrunnelse,
        )
    }

    private fun mapSykmeldingRecordToPersistedSykmeldingMeldinger(
        sykmeldingRecord: SykmeldingRecord
    ): PersistedSykmeldingMeldinger {
        return when (val value = sykmeldingRecord.sykmelding) {
            is DigitalSykmelding -> {
                return PersistedSykmeldingMeldinger(
                    tilNav = value.meldinger.tilNav,
                    tilArbeidsgiver = value.meldinger.tilArbeidsgiver,
                )
            }
            is Papirsykmelding -> {
                return PersistedSykmeldingMeldinger(
                    tilNav = value.bistandNav?.beskrivBistand,
                    tilArbeidsgiver = null,
                )
            }
            is XmlSykmelding -> {
                // skal det være bistand fra nav her (og på papir)? SykmeldingRecordBistandNav.
                // Bruke andreTiltak på arbeidsgiver? eller nulle
                return PersistedSykmeldingMeldinger(
                    tilNav = value.bistandNav?.beskrivBistand,
                    tilArbeidsgiver = null,
                )
            }
            else -> {
                // How should we handle this for other cases such as Utenlandsk? Empty objects?
                return PersistedSykmeldingMeldinger(
                    tilNav = null,
                    tilArbeidsgiver = null,
                )
            }
        }
    }
}
