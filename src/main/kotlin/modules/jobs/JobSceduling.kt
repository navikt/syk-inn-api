package modules.jobs

import core.logger
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStarting
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.plugins.di.dependencies
import kotlinx.coroutines.launch
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
    monitor.subscribe(ApplicationStopping) {
        launch {
            log.info("Job sceduling stopped")
            jobScheduler.stop()
        }
    }

    monitor.subscribe(ApplicationStarted) {
        launch {
            log.info("Job sceduling started")
            jobScheduler.start()
        }
    }
}
