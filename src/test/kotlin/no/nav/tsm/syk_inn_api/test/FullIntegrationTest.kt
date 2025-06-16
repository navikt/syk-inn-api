package no.nav.tsm.syk_inn_api.test

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName

@Testcontainers
abstract class FullIntegrationTest {

    companion object {
        @Container
        private val postgres =
            PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
                withDatabaseName("testdb")
                withUsername("test")
                withPassword("test")
            }

        @Container
        private val kafka =
            ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0")).also {
                it.start()
            }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // Spring DB
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)

            // Flyway
            registry.add("DB_JDBC_URL", postgres::getJdbcUrl)
            registry.add("DB_USERNAME", postgres::getUsername)
            registry.add("DB_PASSWORD", postgres::getPassword)

            // Kafka
            registry.add("kafka.brokers", kafka::getBootstrapServers)

            println("I AM IN DA PROPERTY THINGY ${kafka.bootstrapServers}")
        }
    }
}
