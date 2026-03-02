package no.nav.tsm.modules.sykmeldinger

import java.sql.Connection

class SykmeldingRepo(val connection: Connection) {
    fun test() {
        // hi copilot, executed this:
        val result =
            connection.prepareStatement("SELECT 1").executeQuery()?.let {
                if (it.next()) {
                    it.getString(1)
                } else null
            }

        println("DATABASE!!!!")
        println(result)
    }
}
