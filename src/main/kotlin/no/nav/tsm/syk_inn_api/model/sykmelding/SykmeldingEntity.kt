package no.nav.tsm.syk_inn_api.model.sykmelding

import SykmeldingConverter
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.util.*

@Entity
data class SykmeldingEntity(
    @Id @GeneratedValue(strategy = GenerationType.AUTO) val id: UUID? = null,
    val sykmeldingId: String,
    val pasientFnr: String,
    val sykmelderHpr: String,
    @Convert(converter = SykmeldingConverter::class) val sykmelding: Sykmelding,
    val legekontorOrgnr: String,
    val validertOk: Boolean = false,
)
