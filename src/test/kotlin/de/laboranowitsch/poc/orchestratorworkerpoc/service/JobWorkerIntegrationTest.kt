package de.laboranowitsch.poc.orchestratorworkerpoc.service

import de.laboranowitsch.poc.orchestratorworkerpoc.data.WorkerJobPayload
import de.laboranowitsch.poc.orchestratorworkerpoc.entity.PageData
import de.laboranowitsch.poc.orchestratorworkerpoc.testutil.IntegrationTests
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.time.Duration
import java.util.*

@IntegrationTests
@ActiveProfiles("test", "worker")
class JobWorkerIntegrationTest @Autowired constructor(
    private val sqsTemplate: SqsTemplate,
    @param:Value("\${app.queues.worker-queue}") private val workerQueueName: String,
    @param:Value("\${app.queues.control-queue}") private val controlQueueName: String,
    @MockitoSpyBean private val jobWorker: JobWorker,
) {

    @BeforeEach
    fun beforeEach() {
        // Clear previous invocations so each test starts with a clean spy.
        clearInvocations(jobWorker)
    }

    @Test
    fun `should process message from worker queue`() {
        val payload = createWorkerJobPayload()

        sqsTemplate.send<WorkerJobPayload> { sender ->
            sender.queue(workerQueueName)
                .payload(payload)
                .header("job-id", payload.jobId)
                .header("page-id", payload.pageId)
                .header("Content-Type", "application/json")
        }

        await()
            .atMost(Duration.ofSeconds(15))
            .untilAsserted {
                verify(jobWorker, atLeastOnce()).processPage(
                    any<WorkerJobPayload>(),
                    eq(payload.jobId),
                    eq(payload.pageId),
                    any(),
                )
                
                // Verify PageDoneMessage is sent to control queue
                val controlMessages = sqsTemplate.receiveMany({ options ->
                    options.queue(controlQueueName).maxNumberOfMessages(10)
                }, String::class.java)
                assertThat(controlMessages).isNotEmpty
                // Check that at least one message contains the pageId and FINISHED status
                val hasExpectedMessage = controlMessages.any { msg ->
                    val body = msg.payload
                    body.contains(payload.pageId) && body.contains("FINISHED")
                }
                assertThat(hasExpectedMessage).isTrue()
            }
    }

    @Test
    fun `should handle multiple tasks with different task numbers`() {
        val workerPayload = JOB_ID_PAGE_ID_LIST.map { idPair ->
            createWorkerJobPayload(idPair.first, idPair.second)
        }

        workerPayload.forEach { payload ->
            sqsTemplate.send<WorkerJobPayload> { sender ->
                sender.queue(workerQueueName)
                    .payload(payload)
                    .header("job-id", payload.jobId)
                    .header("page-id", payload.pageId)
                    .header("Content-Type", "application/json")
            }
        }

        await()
            .atMost(Duration.ofSeconds(15))
            .untilAsserted {
                verify(jobWorker, times(3)).processPage(
                    any<WorkerJobPayload>(),
                    eq(JOB_ID.toString()),
                    any(),
                    any(),
                )
                
                // Verify 3 PageDoneMessage messages with FINISHED status are sent to control queue
                val controlMessages = sqsTemplate.receiveMany({ options ->
                    options.queue(controlQueueName).maxNumberOfMessages(10)
                }, String::class.java)
                
                // Count messages that contain jobId and FINISHED status
                val jobMessages = controlMessages.filter { msg ->
                    val body = msg.payload
                    body.contains(JOB_ID.toString()) && body.contains("FINISHED")
                }
                assertThat(jobMessages.size).isGreaterThanOrEqualTo(3)
                
                // Verify all expected page IDs are present in messages
                val allPageIdsPresent = JOB_ID_PAGE_ID_LIST.all { (_, pageId) ->
                    controlMessages.any { it.payload.contains(pageId) }
                }
                assertThat(allPageIdsPresent).isTrue()
            }
    }

    companion object {
        private val JOB_ID = UUID.randomUUID()
        private val PAGE_ID = UUID.randomUUID()
        private val JOB_ID_PAGE_ID_LIST: List<Pair<String, String>> = listOf(
            JOB_ID.toString() to UUID.randomUUID().toString(),
            JOB_ID.toString() to UUID.randomUUID().toString(),
            JOB_ID.toString() to UUID.randomUUID().toString(),
        )

        @JvmStatic
        fun createWorkerJobPayload(
            jobId: String = JOB_ID.toString(),
            pageId: String = PAGE_ID.toString(),
        ) = WorkerJobPayload(
            jobId = jobId,
            pageId = pageId,
            data = PageData(
                itemIds = listOf(UUID.randomUUID(), UUID.randomUUID()),
            ),
        )
    }
}
