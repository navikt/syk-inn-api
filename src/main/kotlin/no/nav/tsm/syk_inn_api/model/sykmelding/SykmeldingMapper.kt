package no.nav.tsm.syk_inn_api.model.sykmelding

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.SykmeldingRecord
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.objectMapper
import org.postgresql.util.PGobject
import org.slf4j.LoggerFactory

class SykmeldingDBMappingException(message: String, ex: Exception) : Exception(message, ex)

object SykmeldingMapper {

    private val logger = LoggerFactory.getLogger(SykmeldingMapper::class.java)

    fun toSykmeldingDB(sykmeldingMedBehandlingsutfall: SykmeldingRecord): SykmeldingDb {
        try {
            return SykmeldingDb(
                sykmeldingId = sykmeldingMedBehandlingsutfall.sykmelding.id,
                pasientIdent = sykmeldingMedBehandlingsutfall.sykmelding.pasient.fnr,
                fom = sykmeldingMedBehandlingsutfall.sykmelding.aktivitetKafka.first().fom,
                tom = sykmeldingMedBehandlingsutfall.sykmelding.aktivitetKafka.last().tom,
                generatedDate = sykmeldingMedBehandlingsutfall.sykmelding.metadata.genDate,
                sykmelding = sykmeldingMedBehandlingsutfall.sykmelding.toPGobject(),
                validation = sykmeldingMedBehandlingsutfall.validation.toPGobject(),
                metadata = sykmeldingMedBehandlingsutfall.metadata.toPGobject(),
            )
        } catch (ex: Exception) {
            throw SykmeldingDBMappingException(
                "Failed to map sykmelding to SykmeldingDB: ${ex.message}",
                ex
            )
        }
    }

    fun toSykmeldingRecord(sykmeldingDb: SykmeldingDb): SykmeldingRecord {
        val sykmelding = sykmeldingDb.sykmelding.value
        val metadata = sykmeldingDb.metadata.value
        val validation = sykmeldingDb.validation.value
        requireNotNull(sykmelding)
        requireNotNull(metadata)
        requireNotNull(validation)
        return SykmeldingRecord(
            sykmelding = objectMapper.readValue(sykmelding),
            metadata = objectMapper.readValue(metadata),
            validation = objectMapper.readValue(validation),
        )
    }
}

fun Any.toPGobject(): PGobject {
    return PGobject().also {
        it.value = objectMapper.writeValueAsString(this)
        it.type = "json"
    }
}
