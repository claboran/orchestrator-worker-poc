package de.laboranowitsch.poc.orchestratorworkerpoc.service

import io.awspring.cloud.sqs.operations.SqsTemplate
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test", "worker")
@TestPropertySource(properties = [
    "app.queues.control-queue=test-control-queue",
    "app.queues.worker-queue=test-worker-queue",
    "spring.cloud.aws.sqs.endpoint=http://localhost:9324",
    "spring.cloud.aws.region.static=us-east-1",
    "spring.cloud.aws.credentials.access-key=test-key",
    "spring.cloud.aws.credentials.secret-key=test-secret"
])
class JobWorkerIntegrationTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun mockJobWorker(): JobWorker = mock()
    }

    @Autowired
    lateinit var sqsTemplate: SqsTemplate

    @Value("\${app.queues.worker-queue}")
    lateinit var workerQueueName: String

    @Autowired
    lateinit var jobWorker: JobWorker

    @Test
    fun `should process message from worker queue`() {
        // Given
        val payload = WorkerJobPayload(
            jobId = "job-123",
            taskId = "task-1",
            data = "some work",
            taskNumber = 1,
            totalTasks = 1
        )

        // When
        sqsTemplate.send { sender ->
            sender.queue(workerQueueName)
                .payload(payload)
                .header("job-id", "job-123")
                .header("task-id", "task-1")
                .header("message-type", "WORKER_TASK")
        }

        // Then
        await()
            .atMost(Duration.ofSeconds(15))
            .untilAsserted {
                verify(jobWorker, atLeastOnce()).processTask(
                    any<WorkerJobPayload>(),
                    eq("job-123"),
                    eq("task-1"),
                    any()
                )
            }
    }

    @Test
    fun `should handle multiple tasks with different task numbers`() {
        // Given
        val tasks = (1..3).map { taskNum ->
            WorkerJobPayload(
                jobId = "job-456",
                taskId = "task-$taskNum",
                data = "work-data-$taskNum",
                taskNumber = taskNum,
                totalTasks = 3
            )
        }

        // When
        tasks.forEach { payload ->
            sqsTemplate.send { sender ->
                sender.queue(workerQueueName)
                    .payload(payload)
                    .header("job-id", payload.jobId)
                    .header("task-id", payload.taskId)
                    .header("message-type", "WORKER_TASK")
            }
        }

        // Then
        await()
            .atMost(Duration.ofSeconds(15))
            .untilAsserted {
                verify(jobWorker, times(3)).processTask(
                    any<WorkerJobPayload>(),
                    eq("job-456"),
                    any(),
                    any()
                )
            }
    }
}
