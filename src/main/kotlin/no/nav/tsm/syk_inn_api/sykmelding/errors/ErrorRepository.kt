package no.nav.tsm.syk_inn_api.sykmelding.errors

import org.springframework.data.repository.CrudRepository

interface ErrorRepository : CrudRepository<KafkaProcessingError, String>
