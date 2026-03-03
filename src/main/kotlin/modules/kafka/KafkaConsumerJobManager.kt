package no.nav.tsm.modules.kafka

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nav.tsm.core.logger

class KafkaConsumerJobManager(
    val sykmeldingerConsumer: KafkaSykmeldingerConsumer,
    val applicationScope: CoroutineScope,
) {
    val logger = logger()

    var job: Job? = null
    val mutex: Mutex = Mutex()

    suspend fun start() {
        logger.info("Starting kafka consumer")

        if (job?.isActive == true) {
            logger.info("Kafka consumer is already running, not starting a new one")
            return
        }

        mutex.withLock {
            if (job?.isActive == true) {
                logger.info(
                    "Kafka consumer was started by another request while waiting for lock, not starting a new one"
                )
                return
            }

            job =
                applicationScope.launch {
                    try {
                        sykmeldingerConsumer.consume()
                    } catch (ex: CancellationException) {
                        logger.info("KafkaConsumerJob was cancelled gracefully", ex)
                    } catch (cause: Exception) {
                        logger.error("KafkaConsumerJob crashed unexpectedly", cause)
                    } finally {
                        logger.info("Job finished or failed, setting job reference to null")

                        // TODO: Figure out if we should restart or something
                        job = null
                    }
                }
        }
    }

    suspend fun stop() {
        if (job == null || job?.isCancelled == true) {
            logger.info("No job was running, nothing to stop")
            return
        }

        logger.info("Job is running, stopping it ...")
        mutex.withLock {
            if (job == null || job?.isCancelled == true) {
                logger.info("Job was already stopped by another request, nothing to stop")
                return
            }

            job?.cancelAndJoin()
            job = null

            logger.info("Job stopped successfully")
        }
    }
}
