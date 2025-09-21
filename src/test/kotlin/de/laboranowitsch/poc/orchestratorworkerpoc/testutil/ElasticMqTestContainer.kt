package de.laboranowitsch.poc.orchestratorworkerpoc.testutil

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import java.net.URI

/**
 * Common ElasticMQ Testcontainers setup to be reused across integration tests.
 */
object ElasticMqTestContainer {
    private val IMAGE: DockerImageName = DockerImageName.parse("softwaremill/elasticmq-native:1.4.10")

    // Fixed type issue with proper GenericContainer type
    val container: GenericContainer<*> = GenericContainer(IMAGE).apply {
        withExposedPorts(9324, 9325)
        waitingFor(Wait.forListeningPort())
        start()
    }

    fun elasticEndpoint(): String = "http://${container.host}:${container.getMappedPort(9324)}"

    /**
     * Register spring properties so Spring Cloud AWS SQS uses our ElasticMQ endpoint.
     */
    fun registerSpringProperties(registry: DynamicPropertyRegistry) {
        val endpoint = elasticEndpoint()
        registry.add("spring.cloud.aws.sqs.endpoint") { endpoint }
        registry.add("spring.cloud.aws.region.static") { "us-east-1" }
        registry.add("spring.cloud.aws.credentials.access-key") { "test-key" }
        registry.add("spring.cloud.aws.credentials.secret-key") { "test-secret" }

        // Ensure queues exist before Spring context initializes listeners
        ensureQueues(endpoint)
    }

    private fun ensureQueues(endpoint: String) {
        try {
            val client = SqsClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-key", "test-secret")
                    )
                )
                .build()

            listOf("job-control-queue", "job-worker-queue").forEach { name ->
                runCatching {
                    client.createQueue { it.queueName(name) }
                }
            }
        } catch (_: Throwable) {
            // Best-effort; the test will fail later if queues truly don't exist
        }
    }
}