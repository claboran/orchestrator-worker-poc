@file:Suppress("unused")
package de.laboranowitsch.poc.orchestratorworkerpoc.testutil

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * Base integration test class that provides both PostgreSQL and ElasticMQ testcontainers.
 * This replaces the previous AbstractElasticMqTest and provides a more streamlined approach
 * using proper testcontainer annotations.
 * 
 * Usage:
 * - Extend this class for any integration test that needs database and/or SQS functionality
 * - Use @SpringBootTest annotation on your test class as usual
 * - Optionally use @ActiveProfiles to specify test profiles
 */
@Testcontainers
abstract class BaseIntegrationTest {
    companion object {
        // PostgreSQL container with standard test database setup
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }

        // ElasticMQ container for SQS functionality
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
            // Configure PostgreSQL properties
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            
            // Configure ElasticMQ properties
            val endpoint = elasticMqEndpoint()
            ElasticMqTestContainer.registerSpringProperties(registry, endpoint)
        }
    }
}