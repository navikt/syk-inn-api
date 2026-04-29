package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.delete

import no.nav.tsm.core.Environment

class SykmeldingDeleteRepo(val environment: Environment) {

    fun deleteStaleSykmeldinger() {
        /*dbQuery {
            SykmeldingTable.deleteWhere {
                [SykmeldingTable.valuesAktivitet]
            }
        }*/
    }
}
