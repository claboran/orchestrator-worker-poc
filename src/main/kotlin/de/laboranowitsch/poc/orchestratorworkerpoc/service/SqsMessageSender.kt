package de.laboranowitsch.poc.orchestratorworkerpoc.service

import com.fasterxml.jackson.databind.ObjectMapper
import de.laboranowitsch.poc.orchestratorworkerpoc.config.AwsCustomConfig
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.LoggingAware
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.logger
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

/**
 * Service that handles sending messages to SQS with proper JSON serialization.
 * This ensures that polymorphic sealed interfaces are correctly serialized with @type discriminators.
 */
@Service
class SqsMessageSender(
    private val sqsTemplate: SqsTemplate,
    @param:Qualifier(AwsCustomConfig.SQS_OBJECT_MAPPER) private val objectMapper: ObjectMapper,
) : LoggingAware {

    /**
     * Sends a typed message to the specified SQS queue.
     * The message is serialized to JSON using the configured ObjectMapper,
     * ensuring proper handling of polymorphic types with @type discriminators.
     */
    fun <T : Any> sendMessage(
        queueName: String,
        message: T,
        headers: Map<String, String> = emptyMap(),
    ) {
        val json = objectMapper.writeValueAsString(message)

        sqsTemplate.send { sender ->
            sender.queue(queueName)
                .payload(json)
                .header("Content-Type", "application/json")
                .apply {
                    headers.forEach { (key, value) ->
                        header(key, value)
                    }
                }
        }

        logger().debug(
            "Sent message of type {} to queue [{}]: {}",
            message::class.simpleName,
            queueName,
            json,
        )
    }
}
