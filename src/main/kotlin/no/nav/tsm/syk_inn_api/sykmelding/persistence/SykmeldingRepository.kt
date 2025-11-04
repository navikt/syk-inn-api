package no.nav.tsm.syk_inn_api.sykmelding.persistence

import java.time.LocalDate
import java.util.*
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface SykmeldingRepository : CrudRepository<SykmeldingDb, UUID> {
    fun findSykmeldingEntityBySykmeldingId(sykmeldingId: String): SykmeldingDb?

    fun deleteBySykmeldingId(sykmeldingId: String)

    fun findAllByPasientIdent(pasientIdent: String): List<SykmeldingDb>

    @Query(
        value =
            """
        SELECT * FROM sykmelding s
        WHERE EXISTS (
            SELECT 1 FROM jsonb_array_elements(s.sykmelding -> 'aktivitet') AS aktivitet
            WHERE (aktivitet ->> 'tom')::date < :cutoffDate
        )
        """,
        nativeQuery = true
    )
    fun findSykmeldingerWithAktivitetOlderThan(
        @Param("cutoffDate") cutoffDate: LocalDate
    ): List<SykmeldingDb>
}
