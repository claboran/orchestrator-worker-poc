@file:Suppress("unused")
package de.laboranowitsch.poc.orchestratorworkerpoc.testutil

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

abstract class AbstractElasticMqTest {
    companion object {
        // Pin to a stable ElasticMQ image tag to avoid surprises from `latest`.
        private val IMAGE = DockerImageName.parse("softwaremill/elasticmq:1.6.0")

        // Start the ElasticMQ container lazily once for all tests extending this base.
        private val container: GenericContainer<*> by lazy {
            GenericContainer(IMAGE).apply {
                withExposedPorts(9324)
                waitingFor(Wait.forListeningPort())
                start()
            }
        }

        // Start the PostgreSQL container lazily once for all tests extending this base.
        private val postgres: PostgreSQLContainer<*> by lazy {
            PostgreSQLContainer("postgres:16").apply {
                withDatabaseName("testdb")
                withUsername("test")
                withPassword("test")
                start()
            }
        }

        // Public accessor so tests can create SDK clients against the container.
        @JvmStatic
        fun elasticEndpoint(): String = "http://${container.host}:${container.getMappedPort(9324)}"

        @JvmStatic
        @Suppress("unused")
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // Configure ElasticMQ properties
            val endpoint = elasticEndpoint()
            ElasticMqTestContainer.registerSpringProperties(registry, endpoint)
            
            // Configure PostgreSQL properties
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
        }
    }
}
