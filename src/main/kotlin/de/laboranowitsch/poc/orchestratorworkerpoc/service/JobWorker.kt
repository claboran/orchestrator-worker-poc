package de.laboranowitsch.poc.orchestratorworkerpoc.service

import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.LoggingAware
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.logger
import io.awspring.cloud.sqs.annotation.SqsListener
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
    @param:Value("\${app.queues.worker-queue}") private val workerQueueName: String,
) : LoggingAware {

    @SqsListener("\${app.queues.worker-queue}")
    fun processTask(
        @Payload payload: WorkerJobPayload,
        @Header("job-id") jobId: String,
        @Header("task-id") taskId: String,
        @Header(value = "message-type", required = false) messageType: String? = null,
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
        },
        onFailure = { error ->
            logger().error(
                "Error processing task [{}] for job [{}]: {}",
                taskId,
                jobId,
                error.message,
                error,
            )
            pushBackToWorkerQueue(jobId, taskId, payload)
        },
    )

    private fun pushBackToWorkerQueue(jobId: String, taskId: String, payload: WorkerJobPayload) = runCatching {
        sqsTemplate.send { sender ->
            sender.queue(workerQueueName)
                .payload(payload)
                .header("job-id", jobId)
                .header("task-id", taskId)
                .header("message-type", "WORKER_TASK_RETRY")
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
