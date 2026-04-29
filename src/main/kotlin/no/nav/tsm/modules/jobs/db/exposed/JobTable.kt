package no.nav.tsm.modules.jobs.db.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object JobTable : Table("job") {
    val name = text("name")
    val desiredState = text("desired_state")
    val updatedAt = timestamp("updated_at")
    val updatedBy = text("updated_by")
}
