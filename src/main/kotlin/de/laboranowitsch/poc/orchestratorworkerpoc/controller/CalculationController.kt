package de.laboranowitsch.poc.orchestratorworkerpoc.controller

import com.fasterxml.jackson.databind.ObjectMapper
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

// DTOs for the REST API
data class StartRequest(val inputs: List<String>)
data class StartResponse(val jobId: String)

@RestController
@RequestMapping("/api/calculate")
class CalculationController(
    private val sqsTemplate: SqsTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${app.queues.control-queue}") private val controlQueueName: String,
) : LoggingAware {

    @PostMapping("/start")
    fun startCalculation(@RequestBody request: StartRequest): ResponseEntity<StartResponse> =
        runCatching {
            val jobId = UUID.randomUUID().toString()
            val payload = StartJobPayload(
                someData = request.inputs.joinToString(","),
                description = "Calculation job with ${request.inputs.size} inputs"
            )

            // Serialize payload to JSON and set Content-Type header for consistent behavior
            val json = objectMapper.writeValueAsString(payload)

            sqsTemplate.send { sender ->
                sender.queue(controlQueueName)
                    .payload(json)
                    .header("job-id", jobId)
                    .header("message-type", "START_JOB")
                    .header("Content-Type", "application/json")
            }

            logger().info("Started calculation job [{}] with inputs: {}", jobId, request.inputs)
            StartResponse(jobId)
        }.fold(
            onSuccess = { response -> ResponseEntity.ok(response) },
            onFailure = { error ->
                logger().error("Failed to start calculation: {}", error.message, error)
                ResponseEntity.internalServerError().build()
            }
        )
}
