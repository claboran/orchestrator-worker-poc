package de.laboranowitsch.poc.orchestratorworkerpoc.controller

import de.laboranowitsch.poc.orchestratorworkerpoc.data.StartJobMessage
import de.laboranowitsch.poc.orchestratorworkerpoc.data.StartResponse
import de.laboranowitsch.poc.orchestratorworkerpoc.service.SqsMessageSender
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.LoggingAware
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api/calculate")
class CalculationController(
    private val sqsMessageSender: SqsMessageSender,
    @param:Value("\${app.queues.control-queue}") private val controlQueueName: String,
) : LoggingAware {

    @PostMapping("/start")
    fun startCalculation(): ResponseEntity<StartResponse> =
        runCatching {
            UUID.randomUUID().toString().let { jobId ->
                val message = StartJobMessage(jobId = jobId)
                sqsMessageSender.sendMessage(
                    queueName = controlQueueName,
                    message = message,
                    headers = mapOf("job-id" to jobId),
                )
                StartResponse(jobId)
            }
        }.fold(
            onSuccess = { response ->
                logger().info("Started calculation job with id [{}]", response)
                ResponseEntity.accepted().body(response)
            },
            onFailure = { error ->
                logger().error("Failed to start calculation: {}", error.message, error)
                ResponseEntity.internalServerError().build()
            },
        )
}
