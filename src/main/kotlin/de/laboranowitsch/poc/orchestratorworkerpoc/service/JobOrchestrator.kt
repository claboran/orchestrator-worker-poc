package de.laboranowitsch.poc.orchestratorworkerpoc.service

import de.laboranowitsch.poc.orchestratorworkerpoc.config.SqsCustomConfig.Companion.SQS_MESSAGE_CONTAINER_FACTORY
import de.laboranowitsch.poc.orchestratorworkerpoc.data.OrchestratorMessage
import de.laboranowitsch.poc.orchestratorworkerpoc.data.PageDoneMessage
import de.laboranowitsch.poc.orchestratorworkerpoc.data.PageStatus.FAILED
import de.laboranowitsch.poc.orchestratorworkerpoc.data.StartJobMessage
import de.laboranowitsch.poc.orchestratorworkerpoc.data.WorkerJobPayload
import de.laboranowitsch.poc.orchestratorworkerpoc.entity.PageData
import de.laboranowitsch.poc.orchestratorworkerpoc.repository.PageStateRepository
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.LoggingAware
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.logger
import io.awspring.cloud.sqs.annotation.SqsListener
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.findByIdOrNull
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Profile("orchestrator") // <<< Only active in orchestrator mode
class JobOrchestrator(
    private val sqsMessageSender: SqsMessageSender,
    private val pocPagePayloadService: PocPagePayloadService,
    private val pageStateRepository: PageStateRepository,
    @param:Value("\${app.queues.worker-queue}") private val workerQueueName: String,
) : LoggingAware {
    @SqsListener(
        value = ["\${app.queues.control-queue}"],
        acknowledgementMode = "MANUAL",
        factory = SQS_MESSAGE_CONTAINER_FACTORY,
    )
    @Transactional
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
            logger().info("Successfully received message job [{}]", jobId)
            acknowledgement.acknowledge()
        },
        onFailure = { error ->
            logger().error("Failed to orchestrate job [{}]: {}", jobId, error.message, error)
        },
    )

    private fun handleStartJob(jobId: String, message: StartJobMessage) {
        logger().info("Starting orchestration for job [{}] with payload jobId: {}", jobId, message.jobId)

        val jobState = pocPagePayloadService.generateForJob(UUID.fromString(jobId))
        pageStateRepository.findByJobStateId(
            jobState?.id
                ?: throw IllegalArgumentException("Job state not found"),
        ).map {
            WorkerJobPayload(
                jobId = jobId,
                pageId = it.id.toString(),
                data = it.data ?: PageData(emptyList()),
            )
        }.forEach { page ->
            sqsMessageSender.sendMessage(
                queueName = workerQueueName,
                message = page,
                headers = mapOf(
                    "job-id" to jobId,
                    "page-id" to page.pageId,
                ),
            )
            logger().info("Sent page [{}] for job [{}] to worker queue", page.pageId, jobId)
        }

        logger().info("Dispatched for job [{}] to worker queue", jobId)
    }

    private fun handlePageDone(jobId: String, message: PageDoneMessage) {
        logger().info(
            "Page [{}] completed for job [{}], success: {}",
            message.pageId,
            jobId,
            message.pageStatus,
        )
        val pageState = pageStateRepository.findByIdOrNull(UUID.fromString(message.pageId)
            ?: throw IllegalArgumentException("Page state not found for id: ${message.pageId}"))
            pageState!!.status = message.pageStatus
        if (message.pageStatus == FAILED) {
            logger().warn("Page [{}] failed: {}", message.pageId, message.errorMessage)
        } else {
            logger().debug("Page [{}] finished successfully", message.pageId)

        }
        pageStateRepository.save(pageState)
    }

}
