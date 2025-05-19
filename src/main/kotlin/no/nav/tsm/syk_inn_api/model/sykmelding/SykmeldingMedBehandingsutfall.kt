package no.nav.tsm.syk_inn_api.model.sykmelding

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import org.postgresql.util.PGobject

/**
 * SykmeldingDb er en databasemodell for sykmeldinger med behandlingsutfall som lagres i lokal
 * PostgreSQL-database.
 */
@Entity
data class SykmeldingDb(
    @Id @GeneratedValue(strategy = GenerationType.AUTO) val id: UUID? = null,
    val sykmeldingId: String,
    val pasientIdent: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val generatedDate: OffsetDateTime?,
    val sykmelding: PGobject,
    val validation: PGobject,
    val metadata: PGobject
)
