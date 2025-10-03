package de.laboranowitsch.poc.orchestratorworkerpoc.service

import com.fasterxml.jackson.databind.ObjectMapper
import de.laboranowitsch.poc.orchestratorworkerpoc.controller.StartJobPayload
import de.laboranowitsch.poc.orchestratorworkerpoc.data.WorkerJobPayload
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.LoggingAware
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.logger
import io.awspring.cloud.sqs.annotation.SqsListener
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
@Profile("orchestrator") // <<< Only active in orchestrator mode
class JobOrchestrator(
    private val sqsTemplate: SqsTemplate,
    private val objectMapper: ObjectMapper,
    @param:Value("\${app.queues.worker-queue}") private val workerQueueName: String,
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

    @SqsListener(
        value = ["\${app.queues.control-queue}"],
        acknowledgementMode = "MANUAL",
    )
    fun orchestrateJob(
        @Payload payload: StartJobPayload,
        @Header("job-id") jobId: String,
        @Header(value = "message-type", required = false) messageType: String? = null,
        acknowledgement: Acknowledgement,
    ) = runCatching {
        logger().info("Orchestrator received job [{}] with message type [{}]", jobId, messageType)

        when (messageType) {
            "START_JOB" -> handleStartJob(jobId, payload)
            null -> throw IllegalArgumentException("Missing message-type header for job $jobId")
            else -> throw IllegalArgumentException("Unsupported message-type '$messageType' for job $jobId")
        }
    }.fold(
        onSuccess = {
            logger().info("Successfully orchestrated job [{}]", jobId)
            acknowledgement.acknowledge()
        },
        onFailure = { error ->
            logger().error("Failed to orchestrate job [{}]: {}", jobId, error.message, error)
        },
    )

    private fun handleStartJob(jobId: String, payload: StartJobPayload) {
        logger().info("Starting orchestration for job [{}] with data: {}", jobId, payload.someData)

        // Generate worker tasks
        val workerTasks = generateWorkerTasks(jobId, payload)

        // Send tasks to worker queue as JSON
        workerTasks.forEach { task ->
            sendJsonMessage(
                workerQueueName,
                task,
                mapOf(
                    "job-id" to jobId,
                    "task-id" to task.taskId,
                    "message-type" to "WORKER_TASK",
                ),
            )
            logger().debug("Sent task [{}] for job [{}] to worker queue", task.taskId, jobId)
        }

        logger().info("Dispatched {} tasks for job [{}] to worker queue", workerTasks.size, jobId)
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
}
