package no.nav.tsm.syk_inn_api.repository

import java.util.*
import no.nav.tsm.syk_inn_api.model.SykmeldingEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Transactional
@Repository
interface SykmeldingRepository : CrudRepository<SykmeldingEntity, UUID> {
    fun findSykmeldingEntityBySykmeldingId(sykmeldingId: String): SykmeldingEntity?

    fun findSykmeldingEntitiesByPasientFnr(pasientFnr: String): List<SykmeldingEntity>
}
