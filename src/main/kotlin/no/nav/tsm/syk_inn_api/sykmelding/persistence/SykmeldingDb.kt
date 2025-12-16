package no.nav.tsm.syk_inn_api.sykmelding.persistence

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
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
    val validertOk: Boolean = false,
    val fom: LocalDate,
    val tom: LocalDate,
    val idempotencyKey: UUID?,
)
