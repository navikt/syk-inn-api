package no.nav.tsm.core.db

import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

suspend fun <T> dbQuery(statement: suspend R2dbcTransaction.() -> T): T = suspendTransaction {
    statement()
}
