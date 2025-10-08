package de.laboranowitsch.poc.orchestratorworkerpoc.service

import de.laboranowitsch.poc.orchestratorworkerpoc.data.PageStatus
import de.laboranowitsch.poc.orchestratorworkerpoc.data.StartJobMessage
import de.laboranowitsch.poc.orchestratorworkerpoc.repository.JobStateRepository
import de.laboranowitsch.poc.orchestratorworkerpoc.repository.PageStateRepository
import de.laboranowitsch.poc.orchestratorworkerpoc.testutil.IntegrationTests
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.util.*

@IntegrationTests
@ActiveProfiles("test", "orchestrator")
class JobOrchestratorControlQueueIntegrationTest @Autowired constructor(
    private val sqsTemplate: SqsTemplate,
    private val sqsMessageSender: SqsMessageSender,
    private val jobStateRepository: JobStateRepository,
    private val pageStateRepository: PageStateRepository,
    @param:Value("\${app.queues.control-queue}") private val controlQueueName: String,
    @param:Value("\${app.queues.worker-queue}") private val workerQueueName: String,
) {

    @Test
    fun `should dispatch worker tasks when receiving a message on control queue`() {
        // Send StartJobMessage - PocPagePayloadService will generate pages automatically
        sqsMessageSender.sendMessage(
            controlQueueName,
            createStartJobMessage(),
            mapOf("job-id" to JOB_ID.toString()),
        )
        // Wait for worker messages to be dispatched (default config generates pages)
        await()
            .atMost(Duration.ofSeconds(60))
            .untilAsserted {
                // Verify job and pages were created by PocPagePayloadService
                val savedJob = jobStateRepository.findByIdOrNull(JOB_ID)
                assertThat(savedJob).isNotNull
                
                val pages = pageStateRepository.findByJobStateId(JOB_ID)
                assertThat(pages).isNotEmpty
                assertThat(pages).allMatch { it.status == PageStatus.CREATED }
                
                val expectedPageCount = pages.size
                
                // Verify worker messages were dispatched for each page
                val messages = sqsTemplate.receiveMany({ options ->
                    options.queue(workerQueueName).maxNumberOfMessages(10)
                }, String::class.java)
                kotlin.test.assertTrue(
                    messages.size >= expectedPageCount,
                    "expected at least $expectedPageCount worker messages, found ${messages.size}",
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