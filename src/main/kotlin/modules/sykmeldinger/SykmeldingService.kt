package modules.sykmeldinger

import no.nav.tsm.modules.sykmeldinger.db.SykmeldingExposedRepo

class SykmeldingService(val repo: SykmeldingExposedRepo) {
    fun test() = repo.test()

    fun createBoio() = repo.createBoio()
}
