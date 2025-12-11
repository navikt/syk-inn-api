package no.nav.tsm.syk_inn_api.sykmelding.scheduled

import no.nav.tsm.sykmelding.input.producer.SykmeldingInputProducer
import org.springframework.stereotype.Service

@Service
class SykmeldingKafkaService(
    private val sykmeldingInputProducer: SykmeldingInputProducer,
    private val sykmeldingStatusRepository: SykmeldingStatusRepository
) {}
