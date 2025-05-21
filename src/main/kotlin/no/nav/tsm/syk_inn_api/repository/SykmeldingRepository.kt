package no.nav.tsm.syk_inn_api.repository

import java.util.*
import no.nav.tsm.syk_inn_api.model.sykmelding.SykmeldingDb
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Transactional
@Repository
interface SykmeldingRepository : CrudRepository<SykmeldingDb, UUID> {
    fun findSykmeldingEntityBySykmeldingId(sykmeldingId: String): SykmeldingDb?

    fun deleteBySykmeldingId(sykmeldingId: String)

    fun findAllByPasientFnr(pasientFnr: String): List<SykmeldingDb>
}
