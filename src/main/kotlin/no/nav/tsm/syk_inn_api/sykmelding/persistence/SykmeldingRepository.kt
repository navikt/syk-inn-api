package no.nav.tsm.syk_inn_api.sykmelding.persistence

import java.time.LocalDate
import java.util.*
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface SykmeldingRepository : CrudRepository<SykmeldingDb, UUID> {
    fun findSykmeldingEntityBySykmeldingId(sykmeldingId: String): SykmeldingDb?

    fun deleteBySykmeldingId(sykmeldingId: String)

    fun findAllByPasientIdent(pasientIdent: String): List<SykmeldingDb>

    fun getSykmeldingDbByIdempotencyKey(idempotencyKey: UUID): SykmeldingDb?

    @Modifying
    @Query(value = "DELETE FROM sykmelding WHERE tom < :cutoffDate", nativeQuery = true)
    fun deleteSykmeldingerWithAktivitetOlderThan(@Param("cutoffDate") cutoffDate: LocalDate): Int
}
