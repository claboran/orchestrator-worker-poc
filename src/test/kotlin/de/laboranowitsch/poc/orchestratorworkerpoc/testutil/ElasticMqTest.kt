package de.laboranowitsch.poc.orchestratorworkerpoc.testutil

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.AnnotationTarget

/**
 * Custom annotation for ElasticMQ (SQS) integration tests with both PostgreSQL and ElasticMQ.
 * This replaces the need to extend AbstractElasticMqTest and provides automatic configuration
 * using Spring Boot's service connection feature where possible.
 * 
 * Usage:
 * ```kotlin
 * @ElasticMqTest
 * class MyQueueTest {
 *     // Test methods here - PostgreSQL and ElasticMQ will be automatically configured
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest
@Testcontainers
annotation class ElasticMqTest

/**
 * Base class for ElasticMQ tests providing both PostgreSQL and ElasticMQ containers.
 * This uses @ServiceConnection for PostgreSQL and @DynamicPropertySource for ElasticMQ.
 */
abstract class ElasticMqTestContainers {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }
        
        @Container
        @JvmStatic
        val elasticMq: GenericContainer<*> = GenericContainer(DockerImageName.parse("softwaremill/elasticmq:1.6.0")).apply {
            withExposedPorts(9324)
            waitingFor(Wait.forListeningPort())
        }
        
        // Public accessor for ElasticMQ endpoint (for tests that need direct SQS client access)
        @JvmStatic
        fun elasticMqEndpoint(): String = "http://${elasticMq.host}:${elasticMq.getMappedPort(9324)}"
        
        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // Configure ElasticMQ properties using the helper
            val endpoint = elasticMqEndpoint()
            ElasticMqTestContainer.registerSpringProperties(registry, endpoint)
        }
    }
}