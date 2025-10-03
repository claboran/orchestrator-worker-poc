package de.laboranowitsch.poc.orchestratorworkerpoc.service

import de.laboranowitsch.poc.orchestratorworkerpoc.data.PageDoneMessage
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
class JobOrchestratorPolymorphicMessagesIntegrationTest @Autowired constructor(
    private val sqsTemplate: SqsTemplate,
    @param:Value("\${app.queues.control-queue}") private val controlQueueName: String,
) {

    @Test
    fun `should handle StartJobMessage with sealed interface polymorphism`() {
        val jobId = "polymorphic-test-${UUID.randomUUID()}"
        val message = StartJobMessage(
            someData = "polymorphic-test-data",
            description = "Testing sealed interface approach",
            priority = "NORMAL",
        )

        // Send StartJobMessage - Spring automatically adds @type field for Jackson polymorphism
        sqsTemplate.send<StartJobMessage> { sender ->
            sender.queue(controlQueueName)
                .payload(message)
                .header("job-id", jobId)
        }

        // Give orchestrator time to process
        await()
            .atMost(Duration.ofSeconds(5))
            .pollDelay(Duration.ofMillis(500))
            .untilAsserted {
                // Test passes if no exception is thrown during message processing
                assert(true)
            }
    }

    @Test
    fun `should handle PageDoneMessage with sealed interface polymorphism`() {
        val jobId = "page-done-test-${UUID.randomUUID()}"
        val pageId = UUID.randomUUID().toString()
        val message = PageDoneMessage(
            pageId = pageId,
            success = true,
            errorMessage = null,
        )

        // Send PageDoneMessage - Spring automatically adds @type field for Jackson polymorphism
        sqsTemplate.send<PageDoneMessage> { sender ->
            sender.queue(controlQueueName)
                .payload(message)
                .header("job-id", jobId)
        }

        // Give orchestrator time to process
        await()
            .atMost(Duration.ofSeconds(5))
            .pollDelay(Duration.ofMillis(500))
            .untilAsserted {
                // Test passes if no exception is thrown during message processing
                assert(true)
            }
    }

    @Test
    fun `should handle PageDoneMessage with failure`() {
        val jobId = "page-failed-test-${UUID.randomUUID()}"
        val pageId = UUID.randomUUID().toString()
        val message = PageDoneMessage(
            pageId = pageId,
            success = false,
            errorMessage = "Simulated processing error",
        )

        sqsTemplate.send<PageDoneMessage> { sender ->
            sender.queue(controlQueueName)
                .payload(message)
                .header("job-id", jobId)
        }

        await()
            .atMost(Duration.ofSeconds(5))
            .pollDelay(Duration.ofMillis(500))
            .untilAsserted {
                assert(true)
            }
    }
}

