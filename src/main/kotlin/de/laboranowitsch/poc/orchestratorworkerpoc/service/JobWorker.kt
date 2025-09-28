package de.laboranowitsch.poc.orchestratorworkerpoc.service

import com.fasterxml.jackson.databind.ObjectMapper
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

/**
 * Worker service that consumes tasks from the worker queue and processes them.
 * For this PoC it simply logs the content. In case of an error, the message is
 * pushed back to the worker queue for retry.
 */
@Service
@Profile("worker")
class JobWorker(
    private val sqsTemplate: SqsTemplate,
    private val objectMapper: ObjectMapper,
    @param:Value("\${app.queues.worker-queue}") private val workerQueueName: String,
) : LoggingAware {

    // Listener now accepts a deserialized WorkerJobPayload directly (awspring's message conversion
    // will convert the JSON body to the target type when Content-Type is application/json).
    @SqsListener(
        value = ["\${app.queues.worker-queue}"],
        acknowledgementMode = "MANUAL",
    )
    fun onWorkerMessage(
        @Payload payload: WorkerJobPayload,
        @Header("job-id") jobId: String,
        @Header("task-id") taskId: String,
        @Header(value = "message-type", required = false) messageType: String? = null,
        acknowledgement: Acknowledgement,
    ) {
        processTask(payload, jobId, taskId, messageType, acknowledgement)
    }

    fun processTask(
        @Payload payload: WorkerJobPayload,
        @Header("job-id") jobId: String,
        @Header("task-id") taskId: String,
        @Header(value = "message-type", required = false) messageType: String? = null,
        acknowledgement: Acknowledgement,
    ) = runCatching {
        logger().info(
            "Worker received task [{}] for job [{}] with type [{}]; data='{}' (task {}/{}). Created at {}",
            taskId,
            jobId,
            messageType,
            payload.data,
            payload.taskNumber,
            payload.totalTasks,
            payload.createdAt,
        )

        // Processing would happen here. For PoC we only log.
    }.fold(
        onSuccess = {
            logger().debug("Successfully processed task [{}] for job [{}]", taskId, jobId)
            acknowledgement.acknowledge()
        },
        onFailure = { error ->
            logger().error(
                "Error processing task [{}] for job [{}]: {}",
                taskId,
                jobId,
                error.message,
                error,
            )
        },
    )

    private fun pushBackToWorkerQueue(jobId: String, taskId: String, payload: WorkerJobPayload) = runCatching {
        val json = objectMapper.writeValueAsString(payload)
        sqsTemplate.send { sender ->
            sender.queue(workerQueueName)
                .payload(json)
                .header("job-id", jobId)
                .header("task-id", taskId)
                .header("message-type", "WORKER_TASK_RETRY")
                .header("Content-Type", "application/json")
        }
        logger().info("Pushed back task [{}] for job [{}] to worker queue", taskId, jobId)
    }.onFailure { sendError ->
        logger().error(
            "Failed to push back task [{}] for job [{}] to worker queue: {}",
            taskId,
            jobId,
            sendError.message,
            sendError,
        )
    }
}
