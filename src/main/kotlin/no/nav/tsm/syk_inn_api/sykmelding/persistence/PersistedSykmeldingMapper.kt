package no.nav.tsm.syk_inn_api.sykmelding.persistence

import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.common.DiagnosekodeMapper
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprGodkjenning
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprKode
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprSykmelder
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprTilleggskompetanse
import no.nav.tsm.syk_inn_api.sykmelding.*
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.Digital
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.EDIEmottak
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.EmottakEnkel
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.Papir
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.PersonIdType
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.DigitalSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.KafkaDiagnoseSystem
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.Papirsykmelding
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecord
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecordAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.UtenlandskSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.XmlSykmelding
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
            hoveddiagnose = payload.value.hoveddiagnose.toPersistedSykmeldingDiagnoseInfo(),
            aktivitet = payload.value.aktivitet.toPersistedSykmeldingAktivitet(),
            pasient = pasient.toPersistedSykmeldingPasient(),
            sykmelder = sykmelder.toPersistedSykmeldingSykmelder(payload.meta.sykmelderHpr),
            bidiagnoser = payload.value.bidiagnoser.toPersistedSykmeldingDiagnoseInfo(),
            svangerskapsrelatert = payload.value.svangerskapsrelatert,
            pasientenSkalSkjermes = payload.value.pasientenSkalSkjermes,
            meldinger = payload.value.meldinger.toPersistedSykmeldingMeldinger(),
            yrkesskade = payload.value.yrkesskade.toPersistedSykmeldingYrkesskade(),
            arbeidsgiver = payload.value.arbeidsgiver.toPersistedSykmeldingArbeidsgiver(),
            tilbakedatering = payload.value.tilbakedatering.toPersistedSykmeldingTilbakedatering(),
            regelResultat = ruleResult.toPersistedSykmeldingRuleResult(),
        )
    }

    fun mapSykmeldingRecordToPersistedSykmelding(
        sykmeldingRecord: SykmeldingRecord,
    ): PersistedSykmelding {
        //TODO utvide...
        val hovedDiagnose =
            requireNotNull(
                sykmeldingRecord.sykmelding.sykmeldingRecordMedisinskVurdering.hovedDiagnose,
            ) {
                "Missing hovedDiagnose in sykmeldingRecord"
            } // TODO("Handle this case - we need to support bidiagnose etc. is it ok to miss
        // hoveddiagnose ? ")

        return PersistedSykmelding(
            hoveddiagnose =
                mapHoveddiagnoseToPersistedSykmeldingHoveddiagnose(
                    hovedDiagnose.system.toDiagnoseSystem(),
                    hovedDiagnose.kode,
                ),
            aktivitet =
                mapSykmeldingRecordAktivitetToPersistedSykmeldingAktivitet(
                    sykmeldingRecord.sykmelding.sykmeldingRecordAktivitet.first(),
                ),
        )
    }

    private fun mapHoveddiagnoseToPersistedSykmeldingHoveddiagnose(
        system: DiagnoseSystem,
        code: String
    ): PersistedSykmeldingDiagnoseInfo {
        return PersistedSykmeldingDiagnoseInfo(
            system = system,
            code = code,
            text = DiagnosekodeMapper.findTextFromDiagnoseSystem(system, code)
                ?: "Unknown diagnosis code: $code",
        )
    }

    private fun OpprettSykmeldingDiagnoseInfo.toPersistedSykmeldingDiagnoseInfo(): PersistedSykmeldingDiagnoseInfo {
        return PersistedSykmeldingDiagnoseInfo(
            system = system,
            code = code,
            text = DiagnosekodeMapper.findTextFromDiagnoseSystem(system, code)
                ?: "Unknown diagnosis code: $code",
        )
    }

    private fun mapSykmeldingRecordAktivitetToPersistedSykmeldingAktivitet(
        sykmeldingRecordAktivitet: SykmeldingRecordAktivitet,
    ): PersistedSykmeldingAktivitet {
        return when (sykmeldingRecordAktivitet) {
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

            is UtenlandskSykmelding -> {
                logger.warn("Sykmelding type is not SykInnSykmelding, cannot map HPR number")
                return "Utenlandsk"
            }
        }
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
            fodselsdato = fodselsdato
        )
    }

    private fun HprSykmelder.toPersistedSykmeldingSykmelder(hprNummer: String): PersistedSykmeldingSykmelder {
        return PersistedSykmeldingSykmelder(
            godkjenninger = godkjenninger.toPersistedSykmeldingHprGodkjenning(),
            ident = this.fnr,
            hprNummer = hprNummer,
            fornavn = this.fornavn,
            mellomnavn = this.mellomnavn,
            etternavn = this.etternavn,
        )
    }

    private fun OpprettSykmeldingMeldinger.toPersistedSykmeldingMeldinger(): PersistedSykmeldingMeldinger {
        return PersistedSykmeldingMeldinger(
            tilNav = tilNav,
            tilArbeidsgiver = tilArbeidsgiver,
        )
    }

    private fun OpprettSykmeldingTilbakedatering?.toPersistedSykmeldingTilbakedatering(): PersistedSykmeldingTilbakedatering? {
        if(this == null) return null

        return PersistedSykmeldingTilbakedatering(
            startdato = startdato,
            begrunnelse = begrunnelse,
        )
    }

    private fun OpprettSykmeldingYrkesskade?.toPersistedSykmeldingYrkesskade(): PersistedSykmeldingYrkesskade? {
        if(this == null) return null

        return PersistedSykmeldingYrkesskade(
            yrkesskade = yrkesskade,
            skadedato = skadedato,
        )
    }

    private fun OpprettSykmeldingArbeidsgiver?.toPersistedSykmeldingArbeidsgiver(): PersistedSykmeldingArbeidsgiver? {
        if(this == null) return null

        return PersistedSykmeldingArbeidsgiver(
            harFlere = harFlere,
            arbeidsgivernavn = arbeidsgivernavn
        )

    }

    private fun List<OpprettSykmeldingDiagnoseInfo>.toPersistedSykmeldingDiagnoseInfo(): List<PersistedSykmeldingDiagnoseInfo> {
        if(this.isEmpty()) return emptyList()
        val diagnoseInfo = mutableListOf<PersistedSykmeldingDiagnoseInfo>()

        this.forEach { info ->
            diagnoseInfo.add(
                PersistedSykmeldingDiagnoseInfo(
                    system = info.system,
                    code = info.code,
                    text = DiagnosekodeMapper.findTextFromDiagnoseSystem(info.system, info.code)
                        ?: "Unknown diagnosis code: $info.code",
                )
            )
        }
        return diagnoseInfo
    }

    private fun List<OpprettSykmeldingAktivitet>.toPersistedSykmeldingAktivitet(): List<PersistedSykmeldingAktivitet> {
        if(this.isEmpty()) return emptyList()

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
                            innspillTilArbeidsgiver = opprettSykmeldingAktivitet.innspillTilArbeidsgiver,
                            fom = opprettSykmeldingAktivitet.fom,
                            tom = opprettSykmeldingAktivitet.tom,
                        ),
                    )

                is OpprettSykmeldingAktivitet.Behandlingsdager ->
                    aktivitet.add(
                        PersistedSykmeldingAktivitet.Behandlingsdager(
                            antallBehandlingsdager = opprettSykmeldingAktivitet.antallBehandlingsdager,
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

    private fun List<HprGodkjenning>.toPersistedSykmeldingHprGodkjenning(): List<PersistedSykmeldingHprGodkjenning> {
        if(this.isEmpty()) return emptyList()

        val hprGodkjenninger = mutableListOf<PersistedSykmeldingHprGodkjenning>()

        this.forEach { godkjenning ->
            hprGodkjenninger.add(PersistedSykmeldingHprGodkjenning(
                helsepersonellkategori = godkjenning.helsepersonellkategori.toPersistedSykmeldingHprKode(),
                autorisasjon = godkjenning.autorisasjon.toPersistedSykmeldingHprKode(),
                tillegskompetanse = godkjenning.tillegskompetanse.toPersistedSykmeldingTilleggskompetanse()
            ))
        }

    }

    private fun RegulaResult.toPersistedSykmeldingRuleResult(): PersistedSykmeldingRuleResult {
        return when(this) {
            is RegulaResult.Ok -> PersistedSykmeldingRuleResult(
                result = this.status.name,
                meldingTilSender = null
            )

            is RegulaResult.NotOk -> PersistedSykmeldingRuleResult(
                result = this.status.name,
                meldingTilSender = outcome.reason.sykmelder
            )
        }
    }
    private fun List<HprTilleggskompetanse>?.toPersistedSykmeldingTilleggskompetanse(): List<PersistedSykmeldingHprTilleggskompetanse>? {
        if(this.isEmpty()) return emptyList()


    }

    private fun HprKode?.toPersistedSykmeldingHprKode(): PersistedSykmeldingHprKode? {
        if(this == null) return null

        return PersistedSykmeldingHprKode(
            aktiv = aktiv,
            oid = oid,
            verdi = verdi
        )
    }
}



