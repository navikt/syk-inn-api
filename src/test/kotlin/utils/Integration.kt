package utils

import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.postgresql.PostgreSQLContainer

abstract class WithPostgresql {
    companion object {
        val postgres = PostgreSQLContainer("postgres:17-alpine").apply { start() }
    }
}

abstract class WithAll : WithPostgresql() {
    companion object {
        val kafka = ConfluentKafkaContainer("confluentinc/cp-kafka:8.1.0").apply { start() }
    }
}
