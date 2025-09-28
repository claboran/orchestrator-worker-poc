package de.laboranowitsch.poc.orchestratorworkerpoc.service

import com.fasterxml.jackson.databind.ObjectMapper
import de.laboranowitsch.poc.orchestratorworkerpoc.controller.StartJobPayload
import de.laboranowitsch.poc.orchestratorworkerpoc.testutil.IntegrationTests
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.util.*

@IntegrationTests
@ActiveProfiles("test", "orchestrator")
class JobOrchestratorControlQueueIntegrationTest @Autowired constructor(
    private val sqsTemplate: SqsTemplate,
    private val objectMapper: ObjectMapper,
    @param:Value("\${app.queues.control-queue}") private val controlQueueName: String,
    @param:Value("\${app.queues.worker-queue}") private val workerQueueName: String,
) {

    @Test
    fun `should dispatch worker tasks when receiving a message on control queue`() {
        val jobId = "test-job-${UUID.randomUUID()}"
        val payload = StartJobPayload(
            someData = "test-data-for-control-queue",
            priority = "HIGH",
            description = "Test job via control queue"
        )

        // Send message directly to control queue
        sqsTemplate.send { sender ->
            sender.queue(controlQueueName)
                .payload(objectMapper.writeValueAsString(payload))
                .header("job-id", jobId)
                .header("message-type", "START_JOB")
                .header("Content-Type", "application/json")
        }

        // Wait for 4 worker messages to be dispatched
        await()
            .atMost(Duration.ofSeconds(60))
            .untilAsserted {
                val messages = sqsTemplate.receiveMany({ options ->
                    options.queue(workerQueueName).maxNumberOfMessages(10)
                }, String::class.java)
                kotlin.test.assertTrue(
                    messages.size >= 4,
                    "expected at least 4 worker messages, found ${messages.size}"
                )
            }
    }

    @Test
    fun `should handle ERROR_RETRY message type on control queue`() {
        val jobId = "retry-job-${UUID.randomUUID()}"
        val payload = StartJobPayload(
            someData = "retry-test-data",
            priority = "NORMAL"
        )

        // Send ERROR_RETRY message to control queue
        sqsTemplate.send { sender ->
            sender.queue(controlQueueName)
                .payload(objectMapper.writeValueAsString(payload))
                .header("job-id", jobId)
                .header("message-type", "ERROR_RETRY")
                .header("Content-Type", "application/json")
        }

        // Wait for 4 worker messages to be dispatched (retry should behave like START_JOB)
        await()
            .atMost(Duration.ofSeconds(60))
            .untilAsserted {
                val messages = sqsTemplate.receiveMany({ options ->
                    options.queue(workerQueueName).maxNumberOfMessages(10)
                }, String::class.java)
                kotlin.test.assertTrue(
                    messages.size >= 4,
                    "expected at least 4 worker messages from retry, found ${messages.size}"
                )
            }
    }

    @Test
    fun `should handle unknown message type by treating it as START_JOB`() {
        val jobId = "unknown-type-job-${UUID.randomUUID()}"
        val payload = StartJobPayload(
            someData = "unknown-type-test-data"
        )

        // Send message with unknown message type to control queue
        sqsTemplate.send { sender ->
            sender.queue(controlQueueName)
                .payload(objectMapper.writeValueAsString(payload))
                .header("job-id", jobId)
                .header("message-type", "UNKNOWN_TYPE")
                .header("Content-Type", "application/json")
        }

        // Wait for 4 worker messages to be dispatched (should default to START_JOB behavior)
        await()
            .atMost(Duration.ofSeconds(60))
            .untilAsserted {
                val messages = sqsTemplate.receiveMany({ options ->
                    options.queue(workerQueueName).maxNumberOfMessages(10)
                }, String::class.java)
                kotlin.test.assertTrue(
                    messages.size >= 4,
                    "expected at least 4 worker messages from unknown type, found ${messages.size}"
                )
            }
    }
}