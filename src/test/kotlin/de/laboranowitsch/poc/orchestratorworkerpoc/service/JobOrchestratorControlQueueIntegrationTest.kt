package de.laboranowitsch.poc.orchestratorworkerpoc.service

import de.laboranowitsch.poc.orchestratorworkerpoc.data.StartJobMessage
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
    @param:Value("\${app.queues.control-queue}") private val controlQueueName: String,
    @param:Value("\${app.queues.worker-queue}") private val workerQueueName: String,
) {

    @Test
    fun `should dispatch worker tasks when receiving a message on control queue`() {

        sqsTemplate.send<StartJobMessage> { sender ->
            sender.queue(controlQueueName)
                .payload(createStartJobMessage())
                .header("job-id", JOB_ID.toString())
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
                    "expected at least 4 worker messages, found ${messages.size}",
                )
            }
    }

    companion object {
        private val JOB_ID: UUID = UUID.randomUUID()

        @JvmStatic
        fun createStartJobMessage() = StartJobMessage(
            jobId = JOB_ID.toString(),
        )
    }
}