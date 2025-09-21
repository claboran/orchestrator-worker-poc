package de.laboranowitsch.poc.orchestratorworkerpoc.controller

import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.LoggingAware
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.logger
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable
import java.util.*

// DTOs for the REST API
data class StartRequest(val inputs: List<String>)
data class StartResponse(val jobId: String)
// DTO for the message payload
data class StartJobPayload(val someData: String) : Serializable // Or use a more specific payload

@RestController
@RequestMapping("/api/calculate")
@Profile("orchestrator") // <<< Changed from web-api to orchestrator
class CalculationController(
    private val sqsTemplate: SqsTemplate,
    @Value("\${app.queues.control-queue}") private val controlQueueName: String,
    // In a real app, you'd inject a repository to save the initial job state
) : LoggingAware {

    @PostMapping("/start")
    fun startCalculation(@RequestBody request: StartRequest): ResponseEntity<StartResponse> =
        UUID.randomUUID().toString().let { jobId ->
            logger().info("API received request to start job [{}]. Enqueuing...", jobId)
            // save the job state to DB as "STARTED" with jobId and request.inputs
            sqsTemplate.send { sender ->
                sender.queue(controlQueueName)
                    .payload(StartJobPayload("Job triggered with ${request.inputs.size} items"))
                    .header("job-id", jobId)
                    .header("message-type", "START_JOB")
            }
            ResponseEntity.accepted().body(StartResponse(jobId))
        }
}
