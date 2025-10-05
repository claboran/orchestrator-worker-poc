package de.laboranowitsch.poc.orchestratorworkerpoc.service

import de.laboranowitsch.poc.orchestratorworkerpoc.data.PageDoneMessage
import de.laboranowitsch.poc.orchestratorworkerpoc.data.PageStatus
import de.laboranowitsch.poc.orchestratorworkerpoc.data.PageStatus.*
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

        sqsTemplate.send<StartJobMessage> { sender ->
            sender.queue(controlQueueName)
                .payload(createStartJobMessage())
                .header("job-id", JOB_ID.toString())
        }

        await()
            .atMost(Duration.ofSeconds(5))
            .pollDelay(Duration.ofMillis(500))
            .untilAsserted {
                assert(true)
            }
    }

    @Test
    fun `should handle PageDoneMessage with sealed interface polymorphism`() {
        val message = createPageDoneMessage(
            status = FINISHED,
            errorMessage = null,
        )

        sqsTemplate.send<PageDoneMessage> { sender ->
            sender.queue(controlQueueName)
                .payload(message)
                .header("job-id", JOB_ID.toString())
        }

        await()
            .atMost(Duration.ofSeconds(5))
            .pollDelay(Duration.ofMillis(500))
            .untilAsserted {
                assert(true)
            }
    }

    @Test
    fun `should handle PageDoneMessage with failure`() {
        val message = createPageDoneMessage(
            status = FAILED,
            errorMessage = "Simulated error message",
        )

        sqsTemplate.send<PageDoneMessage> { sender ->
            sender.queue(controlQueueName)
                .payload(message)
                .header("job-id", JOB_ID.toString())
        }

        await()
            .atMost(Duration.ofSeconds(5))
            .pollDelay(Duration.ofMillis(500))
            .untilAsserted {
                assert(true)
            }
    }

    companion object {
        private val JOB_ID: UUID = UUID.randomUUID()
        private val PAGE_ID: UUID = UUID.randomUUID()

        @JvmStatic
        fun createStartJobMessage() = StartJobMessage(
            jobId = JOB_ID.toString(),
        )

        @JvmStatic
        fun createPageDoneMessage(
            status: PageStatus,
            errorMessage: String? = null,
            jobId: String = JOB_ID.toString(),
            pageId: String = PAGE_ID.toString(),
        ) = PageDoneMessage(
            jobId = jobId,
            pageId = pageId,
            pageStatus = status,
            errorMessage = errorMessage,
        )
    }
}

