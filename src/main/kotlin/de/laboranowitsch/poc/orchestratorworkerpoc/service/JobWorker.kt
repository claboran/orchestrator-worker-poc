package de.laboranowitsch.poc.orchestratorworkerpoc.service

import de.laboranowitsch.poc.orchestratorworkerpoc.config.SqsCustomConfig.Companion.SQS_MESSAGE_CONTAINER_FACTORY
import de.laboranowitsch.poc.orchestratorworkerpoc.data.PageDoneMessage
import de.laboranowitsch.poc.orchestratorworkerpoc.data.PageStatus
import de.laboranowitsch.poc.orchestratorworkerpoc.data.WorkerJobPayload
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.LoggingAware
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.logger
import io.awspring.cloud.sqs.annotation.SqsListener
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
@Profile("worker")
class JobWorker(
    private val sqsMessageSender: SqsMessageSender,
    @param:Value("\${app.queues.control-queue}") private val controlQueueName: String,
) : LoggingAware {
    @SqsListener(
        value = ["\${app.queues.worker-queue}"],
        acknowledgementMode = "MANUAL",
        factory = SQS_MESSAGE_CONTAINER_FACTORY,
        )
    fun onWorkerMessage(
        @Payload payload: WorkerJobPayload,
        @Header("job-id") jobId: String,
        @Header("page-id") pageId: String,
        acknowledgement: Acknowledgement,
    ) {
        processPage(payload, jobId, pageId, acknowledgement)
    }

    fun processPage(
        payload: WorkerJobPayload,
        jobId: String,
        pageId: String,
        acknowledgement: Acknowledgement,
    ) = runCatching {
        logger().info(
            "Worker received page [{}] for job [{}]; data='{}'",
            pageId,
            jobId,
            payload.data,
        )
        PageDoneMessage(
            jobId = payload.jobId,
            pageId = pageId,
            pageStatus = PageStatus.FINISHED,
        )
    }.fold(
        onSuccess = { pageDoneMessage ->
            logger().debug(
                "Successfully processed task [{}] for job [{}]",
                pageDoneMessage.pageId,
                pageDoneMessage.jobId,
            )
            sqsMessageSender.sendMessage(
                queueName = controlQueueName,
                message = pageDoneMessage,
                headers = mapOf(
                    "job-id" to jobId,
                    "page-id" to pageId,
                ),
            )
            acknowledgement.acknowledge()
        },
        onFailure = { error ->
            logger().error(
                "Error processing task [{}] for job [{}]: {}",
                pageId,
                jobId,
                error.message,
                error,
            )
        },
    )
}
