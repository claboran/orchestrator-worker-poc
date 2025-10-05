package de.laboranowitsch.poc.orchestratorworkerpoc.service

import de.laboranowitsch.poc.orchestratorworkerpoc.data.PageStatus
import de.laboranowitsch.poc.orchestratorworkerpoc.repository.JobStateRepository
import de.laboranowitsch.poc.orchestratorworkerpoc.repository.PageStateRepository
import de.laboranowitsch.poc.orchestratorworkerpoc.testutil.IntegrationTests
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.util.*

@IntegrationTests
@ActiveProfiles("test", "orchestrator")
class JobOrchestratorIntegrationTest @Autowired constructor(
    private val rest: TestRestTemplate,
    private val sqsTemplate: SqsTemplate,
    private val jobStateRepository: JobStateRepository,
    private val pageStateRepository: PageStateRepository,
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

        // Extract jobId from response body
        val jobId = response.body
            ?.substringAfter("\"jobId\":\"")
            ?.substringBefore("\"")
            ?: throw IllegalStateException("No jobId in response")

        await()
            .atMost(Duration.ofSeconds(60))
            .untilAsserted {
                // Verify job and pages were created by PocPagePayloadService
                val savedJob = jobStateRepository.findByIdOrNull(UUID.fromString(jobId))
                assertThat(savedJob).isNotNull

                val pages = pageStateRepository.findByJobStateId(UUID.fromString(jobId))
                assertThat(pages).isNotEmpty
                assertThat(pages).allMatch { it.status == PageStatus.CREATED }

                val expectedPageCount = pages.size

                // Verify worker messages were dispatched for each page
                val messages = sqsTemplate.receiveMany({ options ->
                    options.queue(workerQueueName).maxNumberOfMessages(10)
                }, String::class.java)
                kotlin.test.assertTrue(
                    messages.size >= expectedPageCount,
                    "expected at least $expectedPageCount worker messages, found ${messages.size}",
                )
            }
    }
}
