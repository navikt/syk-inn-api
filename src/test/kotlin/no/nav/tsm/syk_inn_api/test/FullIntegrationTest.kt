package no.nav.tsm.syk_inn_api.test

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName

@Testcontainers
abstract class FullIntegrationTest {

    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun cleanDatabase() {
        val tables =
            jdbcTemplate.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname='public'",
                String::class.java
            )

        jdbcTemplate.execute("SET session_replication_role = 'replica';")
        tables.forEach { table ->
            jdbcTemplate.execute("""TRUNCATE TABLE "$table" RESTART IDENTITY CASCADE;""")
        }
        jdbcTemplate.execute("SET session_replication_role = 'origin';")
    }

    companion object {
        @Container
        private val postgres =
            PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
                withDatabaseName("testdb")
                withUsername("test")
                withPassword("test")
                waitingFor(Wait.forListeningPort())
            }

        @Container
        private val kafka =
            ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:8.1.0")).also {
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
