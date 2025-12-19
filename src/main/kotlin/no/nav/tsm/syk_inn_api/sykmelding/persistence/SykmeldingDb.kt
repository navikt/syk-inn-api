package no.nav.tsm.syk_inn_api.sykmelding.persistence

import com.fasterxml.jackson.annotation.JsonSubTypes
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import org.hibernate.annotations.Type

@Entity
@Table(name = "sykmelding")
data class SykmeldingDb(
    @Id val sykmeldingId: String,
    val mottatt: OffsetDateTime,
    val pasientIdent: String,
    val sykmelderHpr: String,
    @Type(JsonBinaryType::class) val sykmelding: PersistedSykmelding,
    val legekontorOrgnr: String?,
    val legekontorTlf: String?,
    val fom: LocalDate,
    val tom: LocalDate,
    @Column(name = "idempotency_key", updatable = false) val idempotencyKey: UUID,
    @Type(JsonBinaryType::class) @JsonSubTypes val validationResult: PersistedValidationResult
)
