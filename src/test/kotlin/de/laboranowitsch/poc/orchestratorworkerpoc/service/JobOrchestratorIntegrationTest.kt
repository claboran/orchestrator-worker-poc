package de.laboranowitsch.poc.orchestratorworkerpoc.service

import de.laboranowitsch.poc.orchestratorworkerpoc.testutil.IntegrationTests
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import java.time.Duration

@IntegrationTests
@ActiveProfiles("test", "orchestrator")
class JobOrchestratorIntegrationTest @Autowired constructor(
    private val rest: TestRestTemplate,
    private val sqsTemplate: SqsTemplate,
    @param:Value("\${app.queues.worker-queue}") private val workerQueueName: String,
) {

    @Test
    fun `should dispatch worker tasks when receiving a start request`() {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val httpEntity = HttpEntity<Void>(headers)

        val response = rest.postForEntity(
            "/api/calculate/start",
            httpEntity,
            String::class.java,
        )

        assert(response.statusCode == HttpStatus.ACCEPTED) {
            "Expected 202 Accepted but got ${response.statusCode}"
        }

        await()
            .atMost(Duration.ofSeconds(60))
            .untilAsserted {
                val messages = sqsTemplate.receiveMany { options ->
                    options.queue(workerQueueName).maxNumberOfMessages(10)
                }
                kotlin.test.assertTrue(
                    messages.size >= 4,
                    "expected at least 4 worker messages, found ${messages.size}",
                )
            }
    }
}
