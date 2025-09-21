package de.laboranowitsch.poc.orchestratorworkerpoc.service

import com.fasterxml.jackson.databind.ObjectMapper
import de.laboranowitsch.poc.orchestratorworkerpoc.controller.StartJobPayload
import de.laboranowitsch.poc.orchestratorworkerpoc.data.ErrorJobPayload
import de.laboranowitsch.poc.orchestratorworkerpoc.data.WorkerJobPayload
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.LoggingAware
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.logger
import io.awspring.cloud.sqs.annotation.SqsListener
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Service
import java.time.Instant

@Service
@Profile("orchestrator") // <<< Only active in orchestrator mode
class JobOrchestrator(
    private val sqsTemplate: SqsTemplate,
    private val objectMapper: ObjectMapper,
    @param:Value("\${app.queues.worker-queue}") private val workerQueueName: String,
    @param:Value("\${app.queues.control-queue}") private val controlQueueName: String,
) : LoggingAware {

    companion object {
        private const val WORKER_TASKS_COUNT = 4
    }

    @Suppress("SpreadOperator")
    fun sendJsonMessage(queue: String, payloadObj: Any, headers: Map<String, String> = emptyMap()) {
        val json = objectMapper.writeValueAsString(payloadObj)
        sqsTemplate.send { sender ->
            val req = sender.queue(queue)
                .payload(json)
            headers.forEach { (k, v) -> req.header(k, v) }
            // set explicit content-type to indicate JSON payload
            req.header("Content-Type", "application/json")
        }
    }

    @SqsListener("\${app.queues.control-queue}")
    fun orchestrateJob(
        payload: StartJobPayload,
        @Header("job-id") jobId: String,
        @Header(value = "message-type", required = false) messageType: String? = null,
    ) = runCatching {
        logger().info("Orchestrator received job [{}] with message type [{}]", jobId, messageType)

        when (messageType) {
            "START_JOB" -> handleStartJob(jobId, payload)
            "ERROR_RETRY" -> handleErrorRetry(jobId, payload)
            else -> {
                logger().warn("Unknown message type [{}] for job [{}], treating as START_JOB", messageType, jobId)
                handleStartJob(jobId, payload)
            }
        }
    }.fold(
        onSuccess = {
            logger().info("Successfully orchestrated job [{}]", jobId)
        },
        onFailure = { error ->
            logger().error("Failed to orchestrate job [{}]: {}", jobId, error.message, error)
            sendErrorToControlQueue(jobId, error.message ?: "Unknown error occurred")
        },
    )

    private fun handleStartJob(jobId: String, payload: StartJobPayload) {
        logger().info("Starting orchestration for job [{}] with data: {}", jobId, payload.someData)

        // Generate worker tasks
        val workerTasks = generateWorkerTasks(jobId, payload)

        // Send tasks to worker queue as JSON
        workerTasks.forEach { task ->
            sendJsonMessage(workerQueueName, task, mapOf(
                "job-id" to jobId,
                "task-id" to task.taskId,
                "message-type" to "WORKER_TASK",
            ))
            logger().debug("Sent task [{}] for job [{}] to worker queue", task.taskId, jobId)
        }

        logger().info("Dispatched {} tasks for job [{}] to worker queue", workerTasks.size, jobId)
    }

    private fun handleErrorRetry(jobId: String, payload: StartJobPayload) {
        logger().info("Retrying failed job [{}]", jobId)
        // For retry, we can implement different logic if needed
        // For now, treat it the same as start job
        handleStartJob(jobId, payload)
    }

    private fun generateWorkerTasks(jobId: String, payload: StartJobPayload): List<WorkerJobPayload> =
        (1..WORKER_TASKS_COUNT).map { taskNumber ->
            WorkerJobPayload(
                jobId = jobId,
                taskId = "$jobId-task-$taskNumber",
                data = "${payload.someData}-part-$taskNumber",
                taskNumber = taskNumber,
                totalTasks = WORKER_TASKS_COUNT,
            )
        }

    private fun sendErrorToControlQueue(jobId: String, errorMessage: String) = runCatching {
        val errorPayload = ErrorJobPayload(
            originalJobId = jobId,
            errorMessage = errorMessage,
        )

        sendJsonMessage(controlQueueName, errorPayload, mapOf(
            "job-id" to jobId,
            "message-type" to "ERROR_RETRY",
        ))

        logger().info("Sent error payload for job [{}] back to control queue", jobId)
    }.onFailure { sendError ->
        logger().error(
            "Failed to send error payload for job [{}] to control queue: {}",
            jobId,
            sendError.message,
            sendError,
        )
    }
}
