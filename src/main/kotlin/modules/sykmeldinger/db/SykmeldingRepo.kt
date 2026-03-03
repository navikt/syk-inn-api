@file:OptIn(ExperimentalUuidApi::class)

package no.nav.tsm.modules.sykmeldinger.db

import java.util.UUID
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import no.nav.tsm.modules.sykmeldinger.db.exposed.SykmeldingExposed
import no.nav.tsm.modules.sykmeldinger.db.exposed.SykmeldingJsonb
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class SykmeldingExposedRepo() {
    fun test(): List<Pair<UUID, SykmeldingJsonb>> = transaction {
        val sykmeldinger: List<Pair<UUID, SykmeldingJsonb>> =
            SykmeldingExposed.selectAll()
                .where { SykmeldingExposed.createdAt lessEq Clock.System.now() }
                .map { it[SykmeldingExposed.id] to it[SykmeldingExposed.data] }

        println("Test exposed query")
        println(sykmeldinger)

        sykmeldinger
    }

    fun createBoio(): List<Pair<UUID, SykmeldingJsonb>> = transaction {
        val uuid = UUID.randomUUID()

        SykmeldingExposed.insertReturning {
                it[id] = uuid
                it[createdAt] = Clock.System.now()
                it[updatedAt] = Clock.System.now()
                it[data] = SykmeldingJsonb(sykmeldingId = uuid.toString())
            }
            .map { it[SykmeldingExposed.id] to it[SykmeldingExposed.data] }
    }
}
