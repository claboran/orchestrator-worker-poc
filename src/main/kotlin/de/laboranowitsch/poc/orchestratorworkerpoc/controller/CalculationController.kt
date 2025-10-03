package de.laboranowitsch.poc.orchestratorworkerpoc.controller

import de.laboranowitsch.poc.orchestratorworkerpoc.data.StartJobMessage
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.LoggingAware
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.logger
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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
    fun startCalculation(@RequestBody request: StartRequest): ResponseEntity<StartResponse> =
        runCatching {
            val jobId = UUID.randomUUID().toString()
            val message = StartJobMessage(
                someData = request.inputs.joinToString(","),
                description = "Calculation job with ${request.inputs.size} inputs",
            )

            // Spring Cloud AWS automatically serializes the message to JSON
            sqsTemplate.send<StartJobMessage> { sender ->
                sender.queue(controlQueueName)
                    .payload(message)
                    .header("job-id", jobId)
            }

            logger().info("Started calculation job [{}] with inputs: {}", jobId, request.inputs)
            StartResponse(jobId)
        }.fold(
            onSuccess = { response -> ResponseEntity.accepted().body(response) },  // 202 Accepted
            onFailure = { error ->
                logger().error("Failed to start calculation: {}", error.message, error)
                ResponseEntity.internalServerError().build()
            },
        )
}
