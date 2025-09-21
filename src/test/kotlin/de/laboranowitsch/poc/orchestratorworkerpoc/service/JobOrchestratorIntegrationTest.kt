package de.laboranowitsch.poc.orchestratorworkerpoc.service

import de.laboranowitsch.poc.orchestratorworkerpoc.controller.StartRequest
import de.laboranowitsch.poc.orchestratorworkerpoc.testutil.AbstractElasticMqTest
import de.laboranowitsch.poc.orchestratorworkerpoc.testutil.ElasticMqTestContainer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import java.net.URI
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test", "orchestrator")
class JobOrchestratorIntegrationTest @Autowired constructor(
    private val rest: TestRestTemplate,
) : AbstractElasticMqTest() {

    @Test
    fun `should dispatch worker tasks when receiving a start request`() {
        val request = StartRequest(inputs = listOf("one", "two", "three"))

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val httpEntity = HttpEntity(request, headers)

        // Call the REST endpoint on the started server (random port handled by TestRestTemplate)
        val resp = rest.postForEntity("/api/calculate/start", httpEntity, String::class.java)
        assert(resp.statusCode.is2xxSuccessful)

        // Build an SQS client pointed at the Testcontainers ElasticMQ
        val endpoint = AbstractElasticMqTest.elasticEndpoint()
        val sqs = SqsClient.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.US_EAST_1)
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("test-key", "test-secret"))
            )
            .build()

        // Resolve the worker queue URL
        val queueUrl = sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(ElasticMqTestContainer.WORKER_QUEUE).build()).queueUrl()

        // Wait until 4 worker messages are available (JobOrchestrator currently dispatches 4 tasks)
        await().atMost(Duration.ofSeconds(60)).untilAsserted {
            val receiveReq = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(1)
                .build()

            val messages = sqs.receiveMessage(receiveReq).messages()
            kotlin.test.assertTrue(messages.size >= 4, "expected at least 4 worker messages, found ${'$'}{messages.size}")
        }

        sqs.close()
    }
}
