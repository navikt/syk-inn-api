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

    @Modifying
    @Query(value = "DELETE FROM sykmelding WHERE tom < :cutoffDate", nativeQuery = true)
    fun deleteSykmeldingerWithAktivitetOlderThan(@Param("cutoffDate") cutoffDate: LocalDate): Int

    // Efficient count queries for metrics collection
    @Query("SELECT COUNT(s) FROM SykmeldingDb s")
    fun countAll(): Long

    @Query("SELECT COUNT(s) FROM SykmeldingDb s WHERE s.validertOk = true")
    fun countValidertOk(): Long

    @Query("SELECT COUNT(s) FROM SykmeldingDb s WHERE s.validertOk = false")
    fun countValidertNotOk(): Long

    @Query("SELECT MIN(s.fom) FROM SykmeldingDb s")
    fun findMinFom(): LocalDate?
}
