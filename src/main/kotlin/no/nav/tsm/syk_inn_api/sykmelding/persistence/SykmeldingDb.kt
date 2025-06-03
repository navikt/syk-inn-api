package no.nav.tsm.syk_inn_api.sykmelding.persistence

import java.util.*
import no.nav.tsm.syk_inn_api.sykmelding.kafka.util.objectMapper
import org.postgresql.util.PGobject
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table(name = "sykmelding")
data class SykmeldingDb(
    @Id val id: UUID? = null,
    val sykmeldingId: String,
    val pasientFnr: String,
    val sykmelderHpr: String,
    val sykmelding: PGobject,
    val legekontorOrgnr: String,
    val validertOk: Boolean = false,
)

fun Any.toPGobject(): PGobject {
    return PGobject().also {
        it.value = objectMapper.writeValueAsString(this)
        it.type = "jsonb"
    }
}

inline fun <reified T> PGobject.fromPGobject(): T {
    require(this.type == "jsonb") { "Unsupported PGobject type: ${this.type}, expected 'jsonb'" }
    return objectMapper.readValue(this.value, T::class.java)
}
