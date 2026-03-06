package modules.sykmeldinger

import java.util.UUID
import modules.sykmeldinger.db.SykmeldingerRepo

class SykmeldingerService(val repo: SykmeldingerRepo) {
    fun test() = repo.test()

    fun createBoio() = repo.createBoio()

    fun create(payload: SykInnSykmelding) {
        TODO("implement")
    }

    fun verify(payload: SykInnSykmelding) {
        TODO("implement")
    }

    fun byId(sykmeldingId: UUID): SykInnSykmelding {
        TODO("implement")
    }

    fun byIdent(ident: String): List<SykInnSykmelding> {
        TODO("implement")
    }
}
