package no.nav.tsm.syk_inn_api.sykmelding.errors

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "kafka_processing_errors")
data class KafkaProcessingError(
    val kafkaOffset: Long,
    val kafkaPartition: Int,
    val key: String,
    val error: String?,
    val stackTrace: String?,
    @Id val partitionOffset: String = "$kafkaPartition:$kafkaOffset",
)
