@file:OptIn(ExperimentalUuidApi::class)

package no.nav.tsm.modules.sykmeldinger.db.exposed

import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.uuid.ExperimentalUuidApi
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb

data class SykmeldingJsonb(val sykmeldingId: String)

object SykmeldingExposed : Table("sykmelding") {
    val id = javaUUID("id")
    val data =
        jsonb(
            "data",
            { exposedJacksonObjectMapper.writeValueAsString(it) },
            { exposedJacksonObjectMapper.readValue<SykmeldingJsonb>(it) },
        )
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
