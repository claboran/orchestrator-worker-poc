package de.laboranowitsch.poc.orchestratorworkerpoc.testutil

import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.LoggingAware
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.logger
import org.springframework.test.context.DynamicPropertyRegistry
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import java.net.URI

/**
 * Lightweight test utility for ElasticMQ-related helpers.
 * The actual Testcontainers GenericContainer is managed by a test base class
 * so multiple test classes can reuse it.
 */
object ElasticMqTestContainer : LoggingAware {
    const val CONTROL_QUEUE = "job-control-queue"
    const val WORKER_QUEUE = "job-worker-queue"

    fun registerSpringProperties(registry: DynamicPropertyRegistry, endpoint: String) {
        registry.add("spring.cloud.aws.sqs.endpoint") { endpoint }
        registry.add("spring.cloud.aws.region.static") { "us-east-1" }
        registry.add("spring.cloud.aws.credentials.access-key") { "test-key" }
        registry.add("spring.cloud.aws.credentials.secret-key") { "test-secret" }

        // Override the queue names to ensure they match what we create
        registry.add("app.queues.control-queue") { CONTROL_QUEUE }
        registry.add("app.queues.worker-queue") { WORKER_QUEUE }

        // Ensure queues exist before Spring context initializes listeners
        ensureQueues(endpoint)
    }

    fun ensureQueues(endpoint: String) {
        runCatching {
            val client = SqsClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-key", "test-secret")
                    )
                )
                .build()

            listOf(CONTROL_QUEUE, WORKER_QUEUE).forEach { name ->
                runCatching {
                    client.createQueue { it.queueName(name) }
                }.onSuccess {
                    logger().info("Created queue: {}", name)
                }.onFailure { error ->
                    logger().error("Failed to create queue {}: {}", name, error.message)
                }
            }
            client.close()
        }.onFailure { error ->
            logger().error("Failed to ensure queues exist: {}", error.message)
        }
    }
}
