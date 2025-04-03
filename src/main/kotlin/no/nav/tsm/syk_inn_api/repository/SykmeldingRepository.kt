package no.nav.tsm.syk_inn_api.repository

import java.util.*
import no.nav.tsm.syk_inn_api.model.SykmeldingDTO
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository interface SykmeldingRepository : CrudRepository<SykmeldingDTO, UUID> {}
