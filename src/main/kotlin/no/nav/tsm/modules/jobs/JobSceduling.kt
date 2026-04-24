package no.nav.tsm.modules.jobs

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStarting
import io.ktor.server.plugins.di.dependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nav.tsm.core.logger
import no.nav.tsm.modules.jobs.service.JobSchedulerService

fun Application.configureJobScheduling() {
    val log = logger()
    val jobScheduler: JobSchedulerService by dependencies

    monitor.subscribe(ApplicationStarting) {
        launch(Dispatchers.IO) {
            log.info("Job scheduling setup")
            jobScheduler.setup()
        }
    }

    monitor.subscribe(ApplicationStarted) {
        launch(Dispatchers.IO) {
            try {
                log.info("Job scheduling started")
                jobScheduler.start()
            } finally {
                withContext(NonCancellable) {
                    log.info("Job scheduling stopping")
                    jobScheduler.stop()
                    log.info("Job scheduling stopped")
                }
            }
        }
    }
}
