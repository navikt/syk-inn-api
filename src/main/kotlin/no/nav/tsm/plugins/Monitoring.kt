package no.nav.tsm.plugins

import io.ktor.server.application.*
import no.nav.tsm.core.db.dbQuery
import no.nav.tsm.ktor.nais.NaisMonitoring

fun Application.configureMonitoring() {
    install(NaisMonitoring) {
        ready {
            check("database ready") {
                try {
                    dbQuery { exec("SELECT 1") }
                    true
                } catch (_: Exception) {
                    false
                }
            }
        }
    }
}
