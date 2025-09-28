package de.laboranowitsch.poc.orchestratorworkerpoc.service

import com.fasterxml.jackson.databind.ObjectMapper
import de.laboranowitsch.poc.orchestratorworkerpoc.data.WorkerJobPayload
import de.laboranowitsch.poc.orchestratorworkerpoc.testutil.IntegrationTests
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.time.Duration

@IntegrationTests
@ActiveProfiles("test", "worker")
class JobWorkerIntegrationTest @Autowired constructor(
    private val sqsTemplate: SqsTemplate,
    private val objectMapper: ObjectMapper,
    @param:Value("\${app.queues.worker-queue}") private val workerQueueName: String,
    @MockitoSpyBean private val jobWorker: JobWorker,
) {

    @BeforeEach
    fun beforeEach() {
        // Clear previous invocations so each test starts with a clean spy.
        clearInvocations(jobWorker)
    }

    @Test
    fun `should process message from worker queue`() {
        val payload = WorkerJobPayload(
            jobId = "job-123",
            taskId = "task-1",
            data = "some work",
            taskNumber = 1,
            totalTasks = 1,
        )
        val json = objectMapper.writeValueAsString(payload)

        sqsTemplate.send { sender ->
            sender.queue(workerQueueName)
                .payload(json)
                .header("job-id", "job-123")
                .header("task-id", "task-1")
                .header("message-type", "WORKER_TASK")
                .header("Content-Type", "application/json")
        }

        await()
            .atMost(Duration.ofSeconds(15))
            .untilAsserted {
                verify(jobWorker, atLeastOnce()).processTask(
                    any<WorkerJobPayload>(),
                    eq("job-123"),
                    eq("task-1"),
                    any(),
                    any(),
                )
            }
    }

    @Test
    fun `should handle multiple tasks with different task numbers`() {
        val tasks = (1..3).map { taskNum ->
            WorkerJobPayload(
                jobId = "job-456",
                taskId = "task-$taskNum",
                data = "work-data-$taskNum",
                taskNumber = taskNum,
                totalTasks = 3,
            )
        }

        tasks.forEach { payload ->
            sqsTemplate.send { sender ->
                sender.queue(workerQueueName)
                    .payload(objectMapper.writeValueAsString(payload))
                    .header("job-id", payload.jobId)
                    .header("task-id", payload.taskId)
                    .header("message-type", "WORKER_TASK")
                    .header("Content-Type", "application/json")
            }
        }

        await()
            .atMost(Duration.ofSeconds(15))
            .untilAsserted {
                verify(jobWorker, times(3)).processTask(
                    any<WorkerJobPayload>(),
                    eq("job-456"),
                    any(),
                    any(),
                    any(),
                )
            }
    }
}
