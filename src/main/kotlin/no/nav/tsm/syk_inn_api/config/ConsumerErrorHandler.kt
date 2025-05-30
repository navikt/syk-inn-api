package no.nav.tsm.syk_inn_api.config

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Component
import org.springframework.util.backoff.FixedBackOff
import org.springframework.util.backoff.FixedBackOff.UNLIMITED_ATTEMPTS

@Component
class ConsumerErrorHandler :
    DefaultErrorHandler(null, FixedBackOff(BACKOFF_INTERVAL, UNLIMITED_ATTEMPTS)) {
    companion object {
        private const val BACKOFF_INTERVAL = 60_000L
    }

    private val log = LoggerFactory.getLogger(ConsumerErrorHandler::class.java)

    override fun handleOne(
        thrownException: java.lang.Exception,
        record: ConsumerRecord<*, *>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
    ): Boolean {
        log.error(
            """
            Feil i prosesseringen av record:
            - Topic: ${record.topic()}
            - Offset: ${record.offset()}
            - Key: ${record.key()}
            - Exception: ${thrownException::class.simpleName}: ${thrownException.message}
            - Cause: ${thrownException.cause?.let { "${it::class.simpleName}: ${it.message}" } ?: "None"}
            """
                .trimIndent(),
            thrownException
        )

        return super.handleOne(thrownException, record, consumer, container)
    }

    override fun handleOtherException(
        thrownException: java.lang.Exception,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
        batchListener: Boolean
    ) {
        log.error("Feil i listener uten noen records", thrownException)
        super.handleOtherException(thrownException, consumer, container, batchListener)
    }

    override fun handleRemaining(
        thrownException: Exception,
        records: MutableList<ConsumerRecord<*, *>>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
    ) {
        records.forEach { record ->
            log.error(
                "Feil i prossesseringen av record med offset: ${record.offset()}, key: ${record.key()} på topic ${record.topic()}",
                thrownException
            )
        }
        if (records.isEmpty()) {
            log.error("Feil i listener uten noen records", thrownException)
        }

        super.handleRemaining(thrownException, records, consumer, container)
    }

    override fun handleBatch(
        thrownException: Exception,
        data: ConsumerRecords<*, *>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
        invokeListener: Runnable,
    ) {
        if (data.isEmpty) {
            log.error("Feil i listener uten noen records", thrownException)
        }
        data.forEach { record ->
            log.error(
                "Feil i prossesseringen av record med offset: ${record.offset()}, key: ${record.key()} på topic ${record.topic()}",
                thrownException
            )
        }
        super.handleBatch(thrownException, data, consumer, container, invokeListener)
    }
}
