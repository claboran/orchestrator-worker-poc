package de.laboranowitsch.poc.orchestratorworkerpoc.controller

import de.laboranowitsch.poc.orchestratorworkerpoc.data.StartJobMessage
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.LoggingAware
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.logger
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

data class StartRequest(val inputs: List<String>)
data class StartResponse(val jobId: String)

@RestController
@RequestMapping("/api/calculate")
class CalculationController(
    private val sqsTemplate: SqsTemplate,
    @param:Value("\${app.queues.control-queue}") private val controlQueueName: String,
) : LoggingAware {

    @PostMapping("/start")
    fun startCalculation(): ResponseEntity<StartResponse> =
        runCatching {
            UUID.randomUUID().toString().let {
                val message = StartJobMessage(jobId = it)
                sqsTemplate.send<StartJobMessage> { sender ->
                    sender.queue(controlQueueName)
                        .payload(message)
                        .header("job-id", it)
                }
                StartResponse(it)
            }
        }.fold(
            onSuccess = { response ->
                logger().info("Started calculation job with id [{}]", response.jobId)
                ResponseEntity.accepted().body(response)
            },
            onFailure = { error ->
                logger().error("Failed to start calculation: {}", error.message, error)
                ResponseEntity.internalServerError().build()
            },
        )
}
