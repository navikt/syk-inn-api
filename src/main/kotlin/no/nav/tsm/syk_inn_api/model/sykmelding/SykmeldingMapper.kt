package no.nav.tsm.syk_inn_api.model.sykmelding

import no.nav.tsm.mottak.sykmelding.model.metadata.Digital
import no.nav.tsm.mottak.sykmelding.model.metadata.EDIEmottak
import no.nav.tsm.mottak.sykmelding.model.metadata.EmottakEnkel
import no.nav.tsm.mottak.sykmelding.model.metadata.Papir
import no.nav.tsm.mottak.sykmelding.model.metadata.PersonIdType
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.AktivitetIkkeMulig
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.AktivitetKafka
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Avventende
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Behandlingsdager
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.DigitalSykmelding
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Gradert
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.MedisinskVurdering
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Papirsykmelding
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Reisetilskudd
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.SykmeldingRecord
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.UtenlandskSykmelding
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.XmlSykmelding
import org.postgresql.util.PGobject
import org.slf4j.LoggerFactory

object SykmeldingMapper {

    private val logger = LoggerFactory.getLogger(SykmeldingMapper::class.java)

    fun mapToSykmeldingDb(
        sykmeldingId: String,
        sykmeldingRecord: SykmeldingRecord,
        validertOk: Boolean
    ): SykmeldingDb {
        return SykmeldingDb(
            sykmeldingId = sykmeldingId,
            pasientFnr = sykmeldingRecord.sykmelding.pasient.fnr,
            sykmelderHpr = mapHprNummer(sykmeldingRecord),
            sykmelding = mapRecordToSykmelding(sykmeldingRecord).toPGobject(),
            legekontorOrgnr = mapLegekontorOrgnr(sykmeldingRecord),
            validertOk = validertOk
        )
    }

    private fun mapToSykmelding(
        digitalSykmelding: DigitalSykmelding
    ): PGobject { // TODO why do i need this ?
        return Sykmelding(
                hoveddiagnose = mapHovedDiagnose(digitalSykmelding.medisinskVurdering),
                aktivitet = mapAktivitetKafka(digitalSykmelding.aktivitetKafka)
            )
            .toPGobject()
    }

    fun mapAktivitetKafka(aktiviteter: List<AktivitetKafka>): Aktivitet {
        val aktivitet =
            aktiviteter.firstOrNull() ?: throw IllegalArgumentException("No aktivitetKafka present")

        return when (aktivitet) {
            is Gradert ->
                Aktivitet.Gradert(
                    grad = aktivitet.grad,
                    fom = aktivitet.fom.toString(),
                    tom = aktivitet.tom.toString(),
                    reisetilskudd = aktivitet.reisetilskudd
                )
            is AktivitetIkkeMulig ->
                Aktivitet.IkkeMulig(
                    fom = aktivitet.fom.toString(),
                    tom = aktivitet.tom.toString()
                    // medisinskArsak and arbeidsrelatertArsak not supported yet
                )
            is Avventende ->
                Aktivitet.Avventende(
                    fom = aktivitet.fom.toString(),
                    tom = aktivitet.tom.toString(),
                    innspillTilArbeidsgiver = aktivitet.innspillTilArbeidsgiver
                )
            is Behandlingsdager ->
                Aktivitet.Behandlingsdager(
                    antallBehandlingsdager = aktivitet.antallBehandlingsdager,
                    fom = aktivitet.fom.toString(),
                    tom = aktivitet.tom.toString()
                )
            is Reisetilskudd ->
                Aktivitet.Reisetilskudd(
                    fom = aktivitet.fom.toString(),
                    tom = aktivitet.tom.toString()
                )
        }
    }

    fun mapHovedDiagnose(medisinskVurdering: MedisinskVurdering): Hoveddiagnose {
        requireNotNull(medisinskVurdering.hovedDiagnose) { "Hoveddiagnose is required" }
        return Hoveddiagnose(
            system = medisinskVurdering.hovedDiagnose.system,
            code = medisinskVurdering.hovedDiagnose.kode,
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

    fun mapRecordToSykmelding(
        sykmeldingRecord: SykmeldingRecord,
    ): Sykmelding {
        val hovedDiagnose =
            requireNotNull(sykmeldingRecord.sykmelding.medisinskVurdering.hovedDiagnose) {
                "Missing hovedDiagnose in sykmeldingRecord"
            } // TODO("Handle this case - we need to support bidiagnose etc. is it ok to miss
        // hoveddiagnose ? ")

        return Sykmelding(
            hoveddiagnose = Hoveddiagnose(system = hovedDiagnose.system, code = hovedDiagnose.kode),
            aktivitet =
                mapToAktivitet(
                    sykmeldingRecord.sykmelding.aktivitetKafka,
                ),
        )
    }

    fun mapToAktivitet(aktiviteter: List<AktivitetKafka>): Aktivitet {
        require(aktiviteter.size == 1) {
            "Expected exactly one aktivitet, but got ${aktiviteter.size}"
        }

        val aktivitet = aktiviteter.first()

        return when (aktivitet) {
            is Gradert ->
                Aktivitet.Gradert(
                    grad = aktivitet.grad,
                    fom = aktivitet.fom.toString(),
                    tom = aktivitet.tom.toString(),
                    reisetilskudd = aktivitet.reisetilskudd
                )
            is AktivitetIkkeMulig ->
                Aktivitet.IkkeMulig(
                    fom = aktivitet.fom.toString(),
                    tom = aktivitet.tom.toString()
                    // TODO: Add mapping for medisinskArsak and arbeidsrelatertArsak if needed
                )
            is Behandlingsdager ->
                Aktivitet.Behandlingsdager(
                    antallBehandlingsdager = aktivitet.antallBehandlingsdager,
                    fom = aktivitet.fom.toString(),
                    tom = aktivitet.tom.toString()
                )
            is Avventende ->
                Aktivitet.Avventende(
                    innspillTilArbeidsgiver = aktivitet.innspillTilArbeidsgiver,
                    fom = aktivitet.fom.toString(),
                    tom = aktivitet.tom.toString()
                )
            is Reisetilskudd ->
                Aktivitet.Reisetilskudd(
                    fom = aktivitet.fom.toString(),
                    tom = aktivitet.tom.toString()
                )
        }
    }
}
