package no.nav.tsm.syk_inn_api.sykmelding.persistence

import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.common.DiagnosekodeMapper
import no.nav.tsm.syk_inn_api.sykmelding.*
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.Digital
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.EDIEmottak
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.EmottakEnkel
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.Papir
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.PersonIdType
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.DigitalSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.Papirsykmelding
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecord
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecordAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.UtenlandskSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.XmlSykmelding
import org.slf4j.LoggerFactory

object PersistedSykmeldingMapper {

    private val logger = LoggerFactory.getLogger(PersistedSykmeldingMapper::class.java)

    fun mapSykmeldingPayloadToPersistedSykmelding(payload: SykmeldingPayload): PersistedSykmelding {
        return PersistedSykmelding(
            hoveddiagnose =
                mapHoveddiagnoseToPersistedSykmeldinghoveddiagnose(
                    system = payload.sykmelding.hoveddiagnose.system,
                    code = payload.sykmelding.hoveddiagnose.code,
                ),
            aktivitet =
                mapOpprettSykmeldingAktivitetToPersistedSykmeldingAktivitet(
                    payload.sykmelding.opprettSykmeldingAktivitet
                ),
        )
    }

    fun mapSykmeldingRecordToPersistedSykmelding(
        sykmeldingRecord: SykmeldingRecord,
    ): PersistedSykmelding {
        val hovedDiagnose =
            requireNotNull(
                sykmeldingRecord.sykmelding.sykmeldingRecordMedisinskVurdering.hovedDiagnose
            ) {
                "Missing hovedDiagnose in sykmeldingRecord"
            } // TODO("Handle this case - we need to support bidiagnose etc. is it ok to miss
        // hoveddiagnose ? ")

        return PersistedSykmelding(
            hoveddiagnose =
                mapHoveddiagnoseToPersistedSykmeldinghoveddiagnose(
                    hovedDiagnose.system,
                    hovedDiagnose.kode
                ),
            aktivitet =
                mapSykmeldingRecordAktivitetToPersistedSykmeldingAktivitet(
                    sykmeldingRecord.sykmelding.sykmeldingRecordAktivitet.first()
                ),
        )
    }

    private fun mapHoveddiagnoseToPersistedSykmeldinghoveddiagnose(
        system: DiagnoseSystem,
        code: String
    ): PersistedSykmeldingHoveddiagnose {
        return PersistedSykmeldingHoveddiagnose(
            system = system,
            code = code,
            text = DiagnosekodeMapper.findTextFromDiagnoseSystem(system.toString(), code)
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

    private fun mapOpprettSykmeldingAktivitetToPersistedSykmeldingAktivitet(
        opprettSykmeldingAktivitet: OpprettSykmeldingAktivitet,
    ): PersistedSykmeldingAktivitet {
        return when (opprettSykmeldingAktivitet) {
            is OpprettSykmeldingAktivitet.IkkeMulig ->
                PersistedSykmeldingAktivitet.IkkeMulig(
                    fom = opprettSykmeldingAktivitet.fom,
                    tom = opprettSykmeldingAktivitet.tom,
                )
            is OpprettSykmeldingAktivitet.Gradert ->
                PersistedSykmeldingAktivitet.Gradert(
                    grad = opprettSykmeldingAktivitet.grad,
                    fom = opprettSykmeldingAktivitet.fom,
                    tom = opprettSykmeldingAktivitet.tom,
                    reisetilskudd = opprettSykmeldingAktivitet.reisetilskudd,
                )
            is OpprettSykmeldingAktivitet.Avventende ->
                PersistedSykmeldingAktivitet.Avventende(
                    innspillTilArbeidsgiver = opprettSykmeldingAktivitet.innspillTilArbeidsgiver,
                    fom = opprettSykmeldingAktivitet.fom,
                    tom = opprettSykmeldingAktivitet.tom,
                )
            is OpprettSykmeldingAktivitet.Behandlingsdager ->
                PersistedSykmeldingAktivitet.Behandlingsdager(
                    antallBehandlingsdager = opprettSykmeldingAktivitet.antallBehandlingsdager,
                    fom = opprettSykmeldingAktivitet.fom,
                    tom = opprettSykmeldingAktivitet.tom,
                )
            is OpprettSykmeldingAktivitet.Reisetilskudd ->
                PersistedSykmeldingAktivitet.Reisetilskudd(
                    fom = opprettSykmeldingAktivitet.fom,
                    tom = opprettSykmeldingAktivitet.tom,
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
}
