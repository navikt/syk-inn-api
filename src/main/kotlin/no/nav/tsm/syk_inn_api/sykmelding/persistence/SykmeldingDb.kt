package no.nav.tsm.syk_inn_api.sykmelding.persistence

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.Type
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "sykmelding")
data class SykmeldingDb(

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    val sykmeldingId: String,
    val mottatt: OffsetDateTime,
    val pasientIdent: String,
    val sykmelderHpr: String,

    @Type(JsonBinaryType::class)
    @JdbcTypeCode(SqlTypes.JSON)
    val sykmelding: PersistedSykmelding,
    val legekontorOrgnr: String?,
    val legekontorTlf: String?,
    val validertOk: Boolean = false,
)
