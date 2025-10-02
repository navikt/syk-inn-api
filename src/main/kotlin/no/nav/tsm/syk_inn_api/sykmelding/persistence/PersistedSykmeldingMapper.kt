package no.nav.tsm.syk_inn_api.sykmelding.persistence

import java.time.LocalDate
import java.time.Month
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
import no.nav.tsm.syk_inn_api.sykmelding.response.SykInnArbeidsrelatertArsakType
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.sykmelding.input.core.model.Aktivitet
import no.nav.tsm.sykmelding.input.core.model.AktivitetIkkeMulig
import no.nav.tsm.sykmelding.input.core.model.ArbeidsgiverInfo
import no.nav.tsm.sykmelding.input.core.model.ArbeidsrelatertArsakType
import no.nav.tsm.sykmelding.input.core.model.Avventende
import no.nav.tsm.sykmelding.input.core.model.Behandlingsdager
import no.nav.tsm.sykmelding.input.core.model.DiagnoseInfo
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding
import no.nav.tsm.sykmelding.input.core.model.EnArbeidsgiver
import no.nav.tsm.sykmelding.input.core.model.FlereArbeidsgivere
import no.nav.tsm.sykmelding.input.core.model.Gradert
import no.nav.tsm.sykmelding.input.core.model.IngenArbeidsgiver
import no.nav.tsm.sykmelding.input.core.model.InvalidRule
import no.nav.tsm.sykmelding.input.core.model.MedisinskVurdering
import no.nav.tsm.sykmelding.input.core.model.Papirsykmelding
import no.nav.tsm.sykmelding.input.core.model.Pasient
import no.nav.tsm.sykmelding.input.core.model.PendingRule
import no.nav.tsm.sykmelding.input.core.model.Reisetilskudd
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.Tilbakedatering
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.XmlSykmelding
import no.nav.tsm.sykmelding.input.core.model.metadata.Digital
import no.nav.tsm.sykmelding.input.core.model.metadata.EDIEmottak
import no.nav.tsm.sykmelding.input.core.model.metadata.Egenmeldt
import no.nav.tsm.sykmelding.input.core.model.metadata.EmottakEnkel
import no.nav.tsm.sykmelding.input.core.model.metadata.KontaktinfoType
import no.nav.tsm.sykmelding.input.core.model.metadata.OrgIdType
import no.nav.tsm.sykmelding.input.core.model.metadata.Papir
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType
import no.nav.tsm.sykmelding.input.core.model.metadata.Utenlandsk

object PersistedSykmeldingMapper {

    private val logger = logger()

    fun mapSykmeldingPayloadToPersistedSykmelding(
        payload: OpprettSykmeldingPayload,
        sykmeldingId: String,
        pasient: Person,
        sykmelder: Sykmelder,
        validation: ValidationResult,
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
            regelResultat = validation.toPersistedSykmeldingResult(),
        )
    }

    fun mapSykmeldingRecordToPersistedSykmelding(
        sykmeldingRecord: SykmeldingRecord,
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

    fun mapLegekontorOrgnr(sykmeldingRecord: SykmeldingRecord): String? {
        return when (val metadata = sykmeldingRecord.metadata) {
            is Digital -> metadata.orgnummer
            is Papir -> metadata.sender.ids.firstOrNull { it.type == OrgIdType.ENH }?.id
            is EmottakEnkel -> metadata.sender.ids.firstOrNull { it.type == OrgIdType.ENH }?.id
            is EDIEmottak -> metadata.sender.ids.firstOrNull { it.type == OrgIdType.ENH }?.id
            is Utenlandsk -> null
            is Egenmeldt -> null
        }
    }

    fun mapLegekontorTlf(sykmeldingRecord: SykmeldingRecord): String? {
        return when (val sykmelding = sykmeldingRecord.sykmelding) {
            is DigitalSykmelding -> {
                sykmelding.behandler.kontaktinfo
                    .firstOrNull { it.type == KontaktinfoType.TLF }
                    ?.value
            }
            is Papirsykmelding -> {
                sykmelding.behandler.kontaktinfo
                    .firstOrNull { it.type == KontaktinfoType.TLF }
                    ?.value
            }
            is XmlSykmelding -> {
                sykmelding.behandler.kontaktinfo
                    .firstOrNull { it.type == KontaktinfoType.TLF }
                    ?.value
            }
            else -> null
        }
    }

    fun mapHprNummer(value: SykmeldingRecord): String? {
        return when (val sykmelding = value.sykmelding) {
            is DigitalSykmelding -> mapFromSykmelder(sykmelding.sykmelder)
            is Papirsykmelding -> mapFromSykmelder(sykmelding.sykmelder)
            is XmlSykmelding -> mapFromSykmelder(sykmelding.sykmelder)
            else -> error("Unable to map hpr number for sykmelding ${value.sykmelding.id}")
        }
    }

    private fun mapFromSykmelder(
        sykmelder: no.nav.tsm.sykmelding.input.core.model.Sykmelder
    ): String? {
        val hprId = sykmelder.ids.find { it.type == PersonIdType.HPR }?.id
        if (hprId != null) {
            return hprId
        }

        val hasFnr = sykmelder.ids.any { it.type == PersonIdType.FNR }
        if (hasFnr) {
            return null
        }

        error(
            "No HPR or FNR found in Sykmelder-object. First id type: ${
                sykmelder.ids.firstOrNull()?.type?.name ?: "none"
            }",
        )
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
            ident = ident,
            hprNummer = hprNummer,
            fornavn = navn?.fornavn,
            mellomnavn = navn?.mellomnavn,
            etternavn = navn?.etternavn,
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
                            medisinskArsak =
                                PersistedSykmeldingMedisinskArsak(
                                    isMedisinskArsak =
                                        opprettSykmeldingAktivitet.medisinskArsak.isMedisinskArsak,
                                ),
                            arbeidsrelatertArsak =
                                PersistedSykmeldingArbeidsrelatertArsak(
                                    isArbeidsrelatertArsak =
                                        opprettSykmeldingAktivitet.arbeidsrelatertArsak
                                            .isArbeidsrelatertArsak,
                                    arbeidsrelaterteArsaker =
                                        opprettSykmeldingAktivitet.arbeidsrelatertArsak
                                            .arbeidsrelaterteArsaker,
                                    annenArbeidsrelatertArsak =
                                        opprettSykmeldingAktivitet.arbeidsrelatertArsak
                                            .annenArbeidsrelatertArsak,
                                ),
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
        List<PersistedSykmeldingAktivitet> =
        this.map { sykmeldingRecordAktivitet ->
            when (sykmeldingRecordAktivitet) {
                is AktivitetIkkeMulig ->
                    PersistedSykmeldingAktivitet.IkkeMulig(
                        fom = sykmeldingRecordAktivitet.fom,
                        tom = sykmeldingRecordAktivitet.tom,
                        medisinskArsak =
                            PersistedSykmeldingMedisinskArsak(
                                isMedisinskArsak = sykmeldingRecordAktivitet.medisinskArsak != null,
                            ),
                        arbeidsrelatertArsak =
                            sykmeldingRecordAktivitet.arbeidsrelatertArsak?.let {
                                PersistedSykmeldingArbeidsrelatertArsak(
                                    isArbeidsrelatertArsak = true,
                                    annenArbeidsrelatertArsak = it.beskrivelse,
                                    arbeidsrelaterteArsaker =
                                        it.arsak.map { arsak ->
                                            when (arsak) {
                                                ArbeidsrelatertArsakType
                                                    .MANGLENDE_TILRETTELEGGING ->
                                                    SykInnArbeidsrelatertArsakType
                                                        .TILRETTELEGGING_IKKE_MULIG
                                                ArbeidsrelatertArsakType.ANNET ->
                                                    SykInnArbeidsrelatertArsakType.ANNET
                                            }
                                        },
                                )
                            }
                                ?: PersistedSykmeldingArbeidsrelatertArsak(
                                    isArbeidsrelatertArsak = false,
                                    arbeidsrelaterteArsaker = emptyList(),
                                    annenArbeidsrelatertArsak = null,
                                ),
                    )
                is Gradert ->
                    PersistedSykmeldingAktivitet.Gradert(
                        grad = sykmeldingRecordAktivitet.grad,
                        fom = sykmeldingRecordAktivitet.fom,
                        tom = sykmeldingRecordAktivitet.tom,
                        reisetilskudd = sykmeldingRecordAktivitet.reisetilskudd,
                    )
                is Avventende ->
                    PersistedSykmeldingAktivitet.Avventende(
                        innspillTilArbeidsgiver = sykmeldingRecordAktivitet.innspillTilArbeidsgiver,
                        fom = sykmeldingRecordAktivitet.fom,
                        tom = sykmeldingRecordAktivitet.tom,
                    )
                is Behandlingsdager ->
                    PersistedSykmeldingAktivitet.Behandlingsdager(
                        antallBehandlingsdager = sykmeldingRecordAktivitet.antallBehandlingsdager,
                        fom = sykmeldingRecordAktivitet.fom,
                        tom = sykmeldingRecordAktivitet.tom,
                    )
                is Reisetilskudd ->
                    PersistedSykmeldingAktivitet.Reisetilskudd(
                        fom = sykmeldingRecordAktivitet.fom,
                        tom = sykmeldingRecordAktivitet.tom,
                    )
            }
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

    private fun ValidationResult.toPersistedSykmeldingResult(): PersistedSykmeldingRuleResult {
        val meldingTilSender =
            when (val latestRule = rules.maxByOrNull { it.timestamp }) {
                is InvalidRule -> latestRule.reason.sykmelder
                is PendingRule -> latestRule.reason.sykmelder
                else -> null
            }
        return PersistedSykmeldingRuleResult(
            result = status,
            meldingTilSender = meldingTilSender,
        )
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
        arbeidsgiver: ArbeidsgiverInfo
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

    private fun Pasient.toPersistedSykmeldingPasient(pasient: Person): PersistedSykmeldingPasient {
        return PersistedSykmeldingPasient(
            navn = pasient.navn,
            ident = fnr,
            fodselsdato = pasient.fodselsdato,
        )
    }

    private fun List<DiagnoseInfo>?.toPersistedSykmeldingDiagnoseInfoList():
        List<PersistedSykmeldingDiagnoseInfo> {
        if (isNullOrEmpty()) return emptyList()
        val diagnoseInfo = mutableListOf<PersistedSykmeldingDiagnoseInfo>()

        forEach { info ->
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
        sykmeldingRecord: SykmeldingRecord
    ): PersistedSykmeldingYrkesskade? {
        val sykmeldingRecordMedisinskVurdering = sykmeldingRecord.sykmelding.medisinskVurdering
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
        if (arbeidsgiver is FlereArbeidsgivere) {
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
        sykmeldingRecord: SykmeldingRecord
    ): PersistedSykmeldingMeldinger {
        return when (val value = sykmeldingRecord.sykmelding) {
            is DigitalSykmelding -> {
                val arbeidsgiver = value.arbeidsgiver
                val bistandNav = value.bistandNav?.beskrivBistand
                mapToPersistedSykmeldingMeldinger(arbeidsgiver, bistandNav)
            }
            is Papirsykmelding -> {
                val arbeidsgiver = value.arbeidsgiver
                val bistandNav = value.bistandNav?.beskrivBistand
                mapToPersistedSykmeldingMeldinger(arbeidsgiver, bistandNav)
            }
            is XmlSykmelding -> {
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

    fun SykmeldingRecord.isBeforeYear(year: Int): Boolean {
        val tom = sykmelding.aktivitet.maxBy { it.tom }.tom
        return tom.isBefore(
            LocalDate.of(
                year,
                Month.JANUARY,
                1,
            ),
        )
    }
}
