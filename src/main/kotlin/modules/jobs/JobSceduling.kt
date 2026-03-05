package modules.jobs

import core.logger
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStarting
import io.ktor.server.plugins.di.dependencies
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import modules.jobs.service.JobScheduler

fun Application.configureJobSceduling() {
    val log = logger()
    val jobScheduler: JobScheduler by dependencies

    monitor.subscribe(ApplicationStarting) {
        launch {
            log.info("Job sceduling setup")
            jobScheduler.setup()
        }
    }

    monitor.subscribe(ApplicationStarted) {
        launch {
            try {
                log.info("Job sceduling started")
                jobScheduler.start()
            } finally {
                withContext(NonCancellable) {
                    log.info("Job sceduling stopping")
                    jobScheduler.stop()
                    log.info("Job sceduling stopped")
                }
            }
        }
    }
}
