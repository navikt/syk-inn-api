@file:OptIn(ExperimentalUuidApi::class)

package no.nav.tsm.modules.sykmeldinger.db

import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import no.nav.tsm.modules.sykmeldinger.db.exposed.SykmeldingExposed
import no.nav.tsm.modules.sykmeldinger.db.exposed.SykmeldingJsonb
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class SykmeldingExposedRepo() {
    fun test() {
        transaction {
            val sykmeldinger: List<Pair<Uuid, SykmeldingJsonb>> =
                SykmeldingExposed.selectAll()
                    .where { SykmeldingExposed.createdAt lessEq Clock.System.now() }
                    .map { it[SykmeldingExposed.id] to it[SykmeldingExposed.data] }

            println("Test exposed query")
            println(sykmeldinger)
        }
    }

    fun createBoio() {
        transaction {
            val uuid = Uuid.random()
            SykmeldingExposed.insert {
                it[id] = uuid
                it[createdAt] = Clock.System.now()
                it[updatedAt] = Clock.System.now()
                it[data] = SykmeldingJsonb(sykmeldingId = uuid.toString())
            }
        }
    }
}
