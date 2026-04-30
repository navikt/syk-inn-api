package no.nav.tsm.utils

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingJsonbNavn
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingJsonbValidationResult
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.earliestFom
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.idempotencyKey
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.latestTom
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.metaMottatt
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.metaPasientIdent
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.metaPasientNavn
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.metaSource
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.rules
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.type
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.valuesAktivitet
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.valuesPasientenSkalSkjermes
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.valuesSvangerskapsrelatert
import no.nav.tsm.sykmelding.input.core.model.RuleType
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.insert

suspend fun R2dbcTransaction.insertDummySykmelding(id: UUID) {
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
