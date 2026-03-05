package modules.jobs.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object JobStatusTable : Table("job_status") {
    val runner = text("runner")
    val job = text("job")
    val state = text("state")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(runner, job)
}
