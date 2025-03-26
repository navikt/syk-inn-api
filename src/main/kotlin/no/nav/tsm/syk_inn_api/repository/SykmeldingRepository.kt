package no.nav.tsm.syk_inn_api.repository

import no.nav.tsm.syk_inn_api.model.SykmeldingDTO
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SykmeldingRepository : CrudRepository<SykmeldingDTO, UUID> {

}
