package no.nav.tsm.utils

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingJsonbNavn
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingJsonbValidationResult
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable
import no.nav.tsm.sykmelding.input.core.model.RuleType
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert

fun JdbcTransaction.insertDummySykmelding(id: UUID) {
    SykmeldingTable.insert {
        it[SykmeldingTable.id] = id
        it[type] = "syk-inn-api-test"
        it[rules] = SykmeldingJsonbValidationResult(type = RuleType.OK, message = null, rule = null)
        it[idempotencyKey] = UUID.randomUUID()
        it[earliestFom] = LocalDate.now()
        it[latestTom] = LocalDate.now()
        it[metaSource] = "syk-inn-api-test (FHIR)"
        it[metaMottatt] = OffsetDateTime.now()
        it[metaPasientNavn] =
            SykmeldingJsonbNavn(
                fornavn = "Fornavn",
                mellomnavn = "Mellomnavn",
                etternavn = "Etternavn",
            )
        it[metaPasientIdent] = "12312312312"
        it[valuesSvangerskapsrelatert] = false
        it[valuesPasientenSkalSkjermes] = false
        it[valuesAktivitet] = emptyList()
    }
}
