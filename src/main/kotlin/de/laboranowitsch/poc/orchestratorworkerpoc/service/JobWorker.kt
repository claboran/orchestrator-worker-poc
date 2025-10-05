package de.laboranowitsch.poc.orchestratorworkerpoc.service

import de.laboranowitsch.poc.orchestratorworkerpoc.data.WorkerJobPayload
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.LoggingAware
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.logger
import io.awspring.cloud.sqs.annotation.SqsListener
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
import org.springframework.context.annotation.Profile
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
@Profile("worker")
class JobWorker : LoggingAware {
    @SqsListener(
        value = ["\${app.queues.worker-queue}"],
        acknowledgementMode = "MANUAL",
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

        // Processing would happen here. For PoC we only log.
    }.fold(
        onSuccess = {
            logger().debug("Successfully processed task [{}] for job [{}]", pageId, jobId)
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
