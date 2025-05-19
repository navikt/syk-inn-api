package no.nav.tsm.syk_inn_api.service

import no.nav.tsm.mottak.sykmelding.model.metadata.Digital
import no.nav.tsm.mottak.sykmelding.model.metadata.EDIEmottak
import no.nav.tsm.mottak.sykmelding.model.metadata.EmottakEnkel
import no.nav.tsm.mottak.sykmelding.model.metadata.Papir
import no.nav.tsm.mottak.sykmelding.model.metadata.PersonIdType
import no.nav.tsm.syk_inn_api.model.sykmelding.Aktivitet
import no.nav.tsm.syk_inn_api.model.sykmelding.Hoveddiagnose
import no.nav.tsm.syk_inn_api.model.sykmelding.SavedSykmelding
import no.nav.tsm.syk_inn_api.model.sykmelding.Sykmelding
import no.nav.tsm.syk_inn_api.model.sykmelding.SykmeldingEntity
import no.nav.tsm.syk_inn_api.model.sykmelding.SykmeldingPayload
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.AktivitetIkkeMulig
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.AktivitetKafka
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Avventende
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Behandlingsdager
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Gradert
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Papirsykmelding
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Reisetilskudd
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.SykInnSykmelding
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.SykmeldingRecord
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.SykmeldingType
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.UtenlandskSykmelding
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.XmlSykmelding
import no.nav.tsm.syk_inn_api.repository.SykmeldingRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.metrics.data.DefaultRepositoryTagsProvider
import org.springframework.stereotype.Service

@Service
class SykmeldingPersistenceService(
    private val sykmeldingRepository: SykmeldingRepository,
    private val repositoryTagsProvider: DefaultRepositoryTagsProvider
) {
    private val logger = LoggerFactory.getLogger(SykmeldingPersistenceService::class.java)

    fun getSykmeldingById(sykmeldingId: String): SykmeldingEntity? {
        return sykmeldingRepository.findSykmeldingEntityBySykmeldingId(sykmeldingId)
    }

    fun save(payload: SykmeldingPayload, sykmeldingId: String): SykmeldingEntity {
        logger.info("Lagrer sykmelding med id=${sykmeldingId}")
        return sykmeldingRepository.save(
            mapToEntity(
                payload = payload,
                sykmeldingId = sykmeldingId,
            ),
        )
    }

    private fun mapToEntity(payload: SykmeldingPayload, sykmeldingId: String): SykmeldingEntity {
        logger.info("Mapping sykmelding til entity")
        return SykmeldingEntity(
            sykmeldingId = sykmeldingId,
            pasientFnr = payload.pasientFnr,
            sykmelderHpr = payload.sykmelderHpr,
            sykmelding = payload.sykmelding,
            legekontorOrgnr = payload.legekontorOrgnr,
        )
    }

    private fun mapToSavedSykmelding(sykmelding: SykmeldingEntity): SavedSykmelding {
        return SavedSykmelding(
            sykmeldingId = sykmelding.sykmeldingId,
            pasientFnr = sykmelding.pasientFnr,
            sykmelderHpr = sykmelding.sykmelderHpr,
            sykmelding = sykmelding.sykmelding,
            legekontorOrgnr = sykmelding.legekontorOrgnr,
        )
    }

    fun getSykmeldingerByIdent(ident: String): List<SykmeldingEntity> {
        return sykmeldingRepository.findSykmeldingEntitiesByPasientFnr(ident)
    }

    fun updateSykmelding(sykmeldingId: String, sykmeldingRecord: SykmeldingRecord?) {
        if (sykmeldingRecord == null) {
            delete(sykmeldingId)
            return
        }

        val sykmeldingEntity = sykmeldingRepository.findSykmeldingEntityBySykmeldingId(sykmeldingId)
        logger.info(
            "is $sykmeldingId equal to ${sykmeldingRecord.sykmelding.id} ?"
        ) // TODO delete this after testing

        if (
            sykmeldingEntity == null && sykmeldingRecord.sykmelding.type != SykmeldingType.DIGITAL
        ) {
            sykmeldingRepository.save(
                SykmeldingEntity(
                    sykmeldingId = sykmeldingId,
                    pasientFnr = sykmeldingRecord.sykmelding.pasient.fnr,
                    sykmelderHpr = mapHprNummer(sykmeldingRecord),
                    sykmelding = mapRecordToSykmelding(sykmeldingRecord),
                    legekontorOrgnr = mapLegekontorOrgnr(sykmeldingRecord),
                ),
            )
            logger.debug("Saved sykmelding with id=${sykmeldingRecord.sykmelding.id}")
        }

        if (sykmeldingRecord.sykmelding.type == SykmeldingType.DIGITAL) {
            val updatedEntity = sykmeldingEntity?.copy(validertOk = true)
            sykmeldingRepository.save(
                updatedEntity
                    ?: SykmeldingEntity(
                        sykmeldingId = sykmeldingId,
                        pasientFnr = sykmeldingRecord.sykmelding.pasient.fnr,
                        sykmelderHpr = mapHprNummer(sykmeldingRecord),
                        sykmelding = mapRecordToSykmelding(sykmeldingRecord),
                        legekontorOrgnr = mapLegekontorOrgnr(sykmeldingRecord),
                    ),
            )
            logger.info("Updated sykmelding with id=${sykmeldingRecord.sykmelding.id}")
        }
    }

    private fun delete(sykmeldingId: String) {
        sykmeldingRepository.deleteBySykmeldingId(sykmeldingId)
        logger.info("Deleted sykmelding with id=$sykmeldingId")
    }

    private fun mapHprNummer(value: SykmeldingRecord): String {
        return when (val sykmelding = value.sykmelding) {
            is SykInnSykmelding -> {
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

    fun mapLegekontorOrgnr(sykmeldingRecord: SykmeldingRecord): String {
        return when (val metadata = sykmeldingRecord.metadata) {
            is Digital -> metadata.orgnummer
            is Papir -> metadata.sender.ids.firstOrNull().let { it?.id }
                    ?: error("No orgnr found in sender object")
            is EmottakEnkel -> metadata.sender.ids.firstOrNull().let { it?.id }
                    ?: error("No orgnr found in sender object")
            is EDIEmottak -> metadata.sender.ids.firstOrNull().let { it?.id }
                    ?: error("No orgnr found in sender object")
            else -> "Missing legekontor orgnr" // is actually required because of EGENMELDT and
        // UTENLANDSK_SYKMELDING as they are possible values in regulus-maximus
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
