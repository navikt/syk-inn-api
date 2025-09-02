package no.nav.tsm.syk_inn_api.sykmelding.persistence

import java.util.*
import org.springframework.data.repository.CrudRepository

interface SykmeldingRepository : CrudRepository<SykmeldingDb, UUID> {
    fun findSykmeldingEntityBySykmeldingId(sykmeldingId: String): SykmeldingDb?

    fun deleteBySykmeldingId(sykmeldingId: String)

    fun findAllByPasientIdent(pasientIdent: String): List<SykmeldingDb>
}
