package no.nav.tsm.syk_inn_api.sykmelding.persistence

import org.springframework.data.repository.CrudRepository
import java.util.*

interface SykmeldingRepository : CrudRepository<SykmeldingDb, UUID> {
    fun findSykmeldingEntityBySykmeldingId(sykmeldingId: String): SykmeldingDb?
    fun deleteBySykmeldingId(sykmeldingId: String)
    fun findAllByPasientIdent(pasientIdent: String): List<SykmeldingDb>
}
