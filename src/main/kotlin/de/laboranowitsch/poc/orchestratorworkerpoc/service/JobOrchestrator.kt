package de.laboranowitsch.poc.orchestratorworkerpoc.service

import de.laboranowitsch.poc.orchestratorworkerpoc.data.OrchestratorMessage
import de.laboranowitsch.poc.orchestratorworkerpoc.data.PageDoneMessage
import de.laboranowitsch.poc.orchestratorworkerpoc.data.StartJobMessage
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
    @param:Value("\${app.queues.worker-queue}") private val workerQueueName: String,
) : LoggingAware {

    companion object {
        private const val WORKER_TASKS_COUNT = 4
    }

    @SqsListener(
        value = ["\${app.queues.control-queue}"],
        acknowledgementMode = "MANUAL",
    )
    fun orchestrateJob(
        @Payload message: OrchestratorMessage,
        @Header("job-id") jobId: String,
        acknowledgement: Acknowledgement,
    ) = runCatching {
        logger().info("Orchestrator received job [{}] with message type [{}]", jobId, message::class.simpleName)

        when (message) {
            is StartJobMessage -> handleStartJob(jobId, message)
            is PageDoneMessage -> handlePageDone(jobId, message)
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

    private fun handleStartJob(jobId: String, message: StartJobMessage) {
        logger().info("Starting orchestration for job [{}] with data: {}", jobId, message.someData)

        // Generate worker tasks
        val workerTasks = generateWorkerTasks(jobId, message)

        // Send tasks to worker queue - Spring Cloud AWS handles JSON serialization automatically
        workerTasks.forEach { task ->
            sqsTemplate.send<WorkerJobPayload> { sender ->
                sender.queue(workerQueueName)
                    .payload(task)
                    .header("job-id", jobId)
                    .header("task-id", task.taskId)
            }
            logger().debug("Sent task [{}] for job [{}] to worker queue", task.taskId, jobId)
        }

        logger().info("Dispatched {} tasks for job [{}] to worker queue", workerTasks.size, jobId)
    }

    private fun handlePageDone(jobId: String, message: PageDoneMessage) {
        logger().info(
            "Page [{}] completed for job [{}], success: {}",
            message.pageId,
            jobId,
            message.success,
        )
        if (!message.success) {
            logger().warn("Page [{}] failed: {}", message.pageId, message.errorMessage)
        }
        // TODO: implement page completion logic (update job state, check if all pages done, etc.)
    }

    private fun generateWorkerTasks(jobId: String, message: StartJobMessage): List<WorkerJobPayload> =
        (1..WORKER_TASKS_COUNT).map { taskNumber ->
            WorkerJobPayload(
                jobId = jobId,
                taskId = "$jobId-task-$taskNumber",
                data = "${message.someData}-part-$taskNumber",
                taskNumber = taskNumber,
                totalTasks = WORKER_TASKS_COUNT,
            )
        }
}
